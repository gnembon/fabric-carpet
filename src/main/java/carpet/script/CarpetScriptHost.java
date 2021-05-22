package carpet.script;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.script.api.Auxiliary;
import carpet.script.argument.FileArgument;
import carpet.script.argument.FunctionArgument;
import carpet.script.bundled.Module;
import carpet.script.command.CommandArgument;
import carpet.script.command.CommandToken;
import carpet.script.exception.CarpetExpressionException;
import carpet.script.exception.ExpressionException;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.InvalidCallbackException;
import carpet.script.utils.AppStoreManager;
import carpet.script.value.EntityValue;
import carpet.script.value.FunctionValue;
import carpet.script.value.ListValue;
import carpet.script.value.MapValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import carpet.utils.CarpetProfiler;
import carpet.utils.Messenger;

import com.google.gson.JsonElement;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.Tag;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.tuple.Pair;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Math.max;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class CarpetScriptHost extends ScriptHost
{
    private final CarpetScriptServer scriptServer;
    ServerCommandSource responsibleSource;

    private Tag globalState;
    private int saveTimeout;
    public boolean persistenceRequired;

    public Map<Value, Value> appConfig;
    public Map<String, CommandArgument> appArgTypes;

    Predicate<ServerCommandSource> commandValidator;
    boolean isRuleApp;
    public AppStoreManager.StoreNode storeSource;

    private CarpetScriptHost(CarpetScriptServer server, Module code, boolean perUser, ScriptHost parent, Map<Value, Value> config, Map<String, CommandArgument> argTypes, Predicate<ServerCommandSource> commandValidator, boolean isRuleApp)
    {
        super(code, perUser, parent);
        this.saveTimeout = 0;
        this.scriptServer = server;
        persistenceRequired = true;
        if (parent == null && code != null) // app, not a global host
        {
            persistenceRequired = false;
            globalState = loadState();
        }
        else if (parent != null)
        {
            persistenceRequired = ((CarpetScriptHost)parent).persistenceRequired;
        }
        appConfig = config;
        appArgTypes = argTypes;
        this.commandValidator = commandValidator;
        this.isRuleApp = isRuleApp;
        storeSource = null;
    }

    public static CarpetScriptHost create(CarpetScriptServer scriptServer, Module module, boolean perPlayer, ServerCommandSource source, Predicate<ServerCommandSource> commandValidator, boolean isRuleApp, AppStoreManager.StoreNode storeSource)
    {
        CarpetScriptHost host = new CarpetScriptHost(scriptServer, module, perPlayer, null, Collections.emptyMap(), new HashMap<>(), commandValidator, isRuleApp);
        // parse code and convert to expression
        if (module != null)
        {
            try
            {
                String code = module.getCode();
                if (code == null)
                {
                    Messenger.m(source, "r Unable to load "+module.getName()+" app - code not found");
                    return null;
                }
                host.setChatErrorSnooper(source);
                CarpetExpression ex = new CarpetExpression(host.main, code, source, new BlockPos(0, 0, 0));
                ex.getExpr().asATextSource();
                host.storeSource = storeSource;
                ex.scriptRunCommand(host, new BlockPos(source.getPosition()));
            }
            catch (CarpetExpressionException e)
            {
                host.handleErrorWithStack("Error while evaluating expression", e);
                host.resetErrorSnooper();
                return null;
            }
            catch (ArithmeticException ae)
            {
                host.handleErrorWithStack("Math doesn't compute", ae);
                return null;
            }
            finally
            {
                host.storeSource = null;
            }
        }
        return host;
    }

    private static int execute(CommandContext<ServerCommandSource> ctx, String hostName, FunctionArgument funcSpec, List<String> paramNames) throws CommandSyntaxException
    {
        CarpetProfiler.ProfilerToken currentSection = CarpetProfiler.start_section(null, "Scarpet command", CarpetProfiler.TYPE.GENERAL);
        CarpetScriptHost cHost = CarpetServer.scriptServer.modules.get(hostName).retrieveOwnForExecution(ctx.getSource());
        List<String> argNames = funcSpec.function.getArguments();
        if ((argNames.size()-funcSpec.args.size()) != paramNames.size())
            throw new SimpleCommandExceptionType(new LiteralText("Target function "+funcSpec.function.getPrettyString()+" as wrong number of arguments, required "+paramNames.size()+", found "+argNames.size()+" with "+funcSpec.args.size()+" provided")).create();
        List<Value> args = new ArrayList<>(argNames.size());
        for (String s : paramNames)
        {
            args.add(CommandArgument.getValue(ctx, s, cHost));
        }
        args.addAll(funcSpec.args);
        Value response = cHost.handleCommand(ctx.getSource(), funcSpec.function, args);
        // will skip prints for new
        //if (!response.isNull()) Messenger.m(ctx.getSource(), "gi " + response.getString());
        int intres = (int) response.readInteger();
        CarpetProfiler.end_current_section(currentSection);
        return intres;
    }

    public LiteralArgumentBuilder<ServerCommandSource> addPathToCommand(
            LiteralArgumentBuilder<ServerCommandSource> command,
            List<CommandToken> path,
            FunctionArgument functionSpec
    ) throws CommandSyntaxException
    {
        String hostName = main.getName();
        List<String> commandArgs = path.stream().filter(t -> t.isArgument).map(t -> t.surface).collect(Collectors.toList());
        if (commandArgs.size() != (functionSpec.function.getNumParams()-functionSpec.args.size()) )
            throw CommandArgument.error("Number of parameters in function "+functionSpec.function.fullName()+" doesn't match parameters for a command");
        if (path.isEmpty())
        {
            return command.executes((c) -> execute(c, hostName, functionSpec, Collections.emptyList()));
        }
        List<CommandToken> reversedPath = new ArrayList<>(path);
        Collections.reverse(reversedPath);
        ArgumentBuilder<ServerCommandSource, ?> argChain = reversedPath.get(0).getCommandNode(this).executes(c -> execute(c, hostName, functionSpec, commandArgs));
        for (int i = 1; i < reversedPath.size(); i++)
        {
            argChain = reversedPath.get(i).getCommandNode(this).then(argChain);
        }
        return command.then(argChain);
    }

    public LiteralArgumentBuilder<ServerCommandSource> getNewCommandTree(
            List<Pair<List<CommandToken>,FunctionArgument>> entries, Predicate<ServerCommandSource> useValidator
    ) throws CommandSyntaxException
    {
        String hostName = main.getName();
        Predicate<ServerCommandSource> configValidator = getCommandConfigPermissions();
        LiteralArgumentBuilder<ServerCommandSource> command = literal(hostName).
               requires((player) -> CarpetServer.scriptServer.modules.containsKey(hostName) && useValidator.test(player) && configValidator.test(player));
        for (Pair<List<CommandToken>,FunctionArgument> commandData : entries)
        {
            command = this.addPathToCommand(command, commandData.getKey(), commandData.getValue());
        }
        return command;
    }

    public Predicate<ServerCommandSource> getCommandConfigPermissions() throws CommandSyntaxException
    {
        Value confValue = appConfig.get(StringValue.of("command_permission"));
        if (confValue == null) return s -> true;
        if (confValue instanceof NumericValue)
        {
            int level = ((NumericValue) confValue).getInt();
            if (level < 1 || level > 4) throw CommandArgument.error("Numeric permission level for custom commands should be between 1 and 4");
            return s -> s.hasPermissionLevel(level);
        }
        if (!(confValue instanceof FunctionValue))
        {
            String perm = confValue.getString().toLowerCase(Locale.ROOT);
            switch (perm)
            {
                case "ops": return s -> s.hasPermissionLevel(2);
                case "server": return s -> !(s.getEntity() instanceof ServerPlayerEntity);
                case "players": return s -> s.getEntity() instanceof ServerPlayerEntity;
                case "all": return s -> true;
                default: throw CommandArgument.error("Unknown command permission: "+perm);
            }
        }
        FunctionValue fun = (FunctionValue) confValue;
        if (fun.getNumParams() != 1) throw CommandArgument.error("Custom command permission function should expect 1 argument");
        String hostName = getName();
        return s -> {
            try
            {
                CarpetProfiler.ProfilerToken currentSection = CarpetProfiler.start_section(null, "Scarpet command", CarpetProfiler.TYPE.GENERAL);
                CarpetScriptHost cHost = null;
                cHost = CarpetServer.scriptServer.modules.get(hostName).retrieveOwnForExecution(s);
                Value response = cHost.handleCommand(s, fun, Collections.singletonList(
                        (s.getEntity() instanceof ServerPlayerEntity)?new EntityValue(s.getEntity()):Value.NULL)
                );
                boolean res = response.getBoolean();
                CarpetProfiler.end_current_section(currentSection);
                return res;
            }
            catch (CommandSyntaxException e)
            {
                Messenger.m(s, "rb Unable to run app command: "+e.getMessage());
                return false;
            }
        };
    }

    @Override
    protected ScriptHost duplicate()
    {
        return new CarpetScriptHost(scriptServer, main, false, this, appConfig, appArgTypes, commandValidator, isRuleApp);
    }

    @Override
    protected void setupUserHost(ScriptHost host)
    {
        super.setupUserHost(host);
        // transfer Events
        CarpetScriptHost child = (CarpetScriptHost) host;
        CarpetEventServer.Event.transferAllHostEventsToChild(child);
        FunctionValue onStart = child.getFunction("__on_start");
        if (onStart != null) child.callNow(onStart, Collections.emptyList());
    }

    @Override
    public void addUserDefinedFunction(Context ctx, Module module, String funName, FunctionValue function)
    {
        super.addUserDefinedFunction(ctx, module, funName, function);
        if (ctx.host.main != module) return; // not dealing with automatic imports / exports /configs / apps from imports
        if (funName.startsWith("__")) // potential fishy activity
        {
            if (funName.startsWith("__on_")) // here we can make a determination if we want to only accept events from main module.
            {
                // this is nasty, we have the host and function, yet we add it via names, but hey - works for now
                String event = funName.replaceFirst("__on_", "");
                if (CarpetEventServer.Event.byName.containsKey(event))
                    scriptServer.events.addBuiltInEvent(event, this, function, null);
            }
            else if (funName.equals("__config"))
            {
                // needs to be added as we read the code, cause other events may be affected.
                if (!readConfig())
                    throw new InternalExpressionException("Invalid app config (via '__config()' function)");
            }
        }
    }

    private boolean readConfig()
    {
        try
        {
            FunctionValue configFunction = getFunction("__config");
            if (configFunction == null) return false;
            Value ret = callNow(configFunction, Collections.emptyList());
            if (!(ret instanceof MapValue)) return false;
            Map<Value, Value> config = ((MapValue) ret).getMap();
            setPerPlayer(config.getOrDefault(new StringValue("scope"), new StringValue("player")).getString().equalsIgnoreCase("player"));
            persistenceRequired = config.getOrDefault(new StringValue("stay_loaded"), Value.TRUE).getBoolean();
            // check requires
            Value loadRequirements = config.get(new StringValue("requires"));
            if (loadRequirements instanceof FunctionValue)
            {
                Value reqResult = callNow((FunctionValue) loadRequirements, Collections.emptyList());
                if (reqResult.getBoolean()) // != false or null
                    throw new InternalExpressionException(reqResult.getString());
            }
            else
            {
                checkModVersionRequirements(loadRequirements);
            };
            if (storeSource != null)
            {
                Value resources = config.get(new StringValue("resources"));
                if (resources != null)
                {
                    if (!(resources instanceof ListValue)) throw new InternalExpressionException("App resources not defined as a list");
                    for (Value resource : ((ListValue) resources).getItems())
                    {
                        AppStoreManager.addResource(this, storeSource, resource);
                    }
                }
            }
            appConfig = config;
        }
        catch (NullPointerException ignored)
        {
            return false;
        }
        return true;
    }

    static class ListComparator<T extends Comparable<T>> implements Comparator<Pair<List<T>,?>>
    {
        @Override
        public int compare(Pair<List<T>,?> p1, Pair<List<T>,?> p2) {
            List<T> o1 = p1.getKey();
            List<T> o2 = p2.getKey();
            for (int i = 0; i < Math.min(o1.size(), o2.size()); i++) {
                int c = o1.get(i).compareTo(o2.get(i));
                if (c != 0) {
                    return c;
                }
            }
            return Integer.compare(o1.size(), o2.size());
        }
    }

    public void readCustomArgumentTypes() throws CommandSyntaxException
    {
        // read custom arguments
        Value arguments = appConfig.get(StringValue.of("arguments"));
        if (arguments != null)
        {
            if (!(arguments instanceof MapValue))
                throw CommandArgument.error("'arguments' element in config should be a map");
            appArgTypes.clear();
            for (Map.Entry<Value, Value> typeData : ((MapValue)arguments).getMap().entrySet())
            {
                String argument = typeData.getKey().getString();
                Value spec = typeData.getValue();
                if (!(spec instanceof MapValue)) throw CommandArgument.error("Spec for '"+argument+"' should be a map");
                Map<String, Value> specData = ((MapValue) spec).getMap().entrySet().stream().collect(Collectors.toMap(e -> e.getKey().getString(), Map.Entry::getValue));
                appArgTypes.put(argument, CommandArgument.buildFromConfig(argument, specData, this));
            }
        }
    }

    public Boolean addAppCommands(Consumer<Text> notifier)
    {
        try
        {
            readCustomArgumentTypes();
        }
        catch (CommandSyntaxException e)
        {
            notifier.accept(Messenger.c("Error when handling of setting up custom argument types: "+e.getMessage()));
            return false;
        }
        if (appConfig.get(StringValue.of("commands")) != null)
        {
            try
            {
                LiteralArgumentBuilder<ServerCommandSource> command = readCommands(commandValidator);
                if (command != null)
                {
                    scriptServer.server.getCommandManager().getDispatcher().register(command);
                    return true;
                }
                else {
                    return false;
                }
            }
            catch (CommandSyntaxException cse)
            {
                // failed
                notifier.accept(Messenger.c("r Failed to build command system for "+getName()+" thus failed to load the app: ", cse.getRawMessage()));
                return null;
            }

        }
        return addLegacyCommand(notifier);
    }
    
    // Prefixes based on FLoader's SemanticVersionPredicateParser's class prefixes under Apache 2.0, since that's not public API
    private static final Map<String, BiPredicate<SemanticVersion, SemanticVersion>> VERSION_PREFIXES = new HashMap<String, BiPredicate<SemanticVersion, SemanticVersion>>() {{
            put(">=", (target, source) -> source.compareTo(target) >= 0);
            put("<=", (target, source) -> source.compareTo(target) <= 0);
            put(">", (target, source) -> source.compareTo(target) > 0);
            put("<", (target, source) -> source.compareTo(target) < 0);
            put("=", (target, source) -> source.compareTo(target) == 0);
            put("~", (target, source) -> source.compareTo(target) >= 0
                    && source.getVersionComponent(0) == target.getVersionComponent(0)
                    && source.getVersionComponent(1) == target.getVersionComponent(1));
            put("^", (target, source) -> source.compareTo(target) >= 0
                    && source.getVersionComponent(0) == target.getVersionComponent(0));
    }};
    
    public void checkModVersionRequirements(Value reqs) {
        if (reqs == null)
            return;
        if (!(reqs instanceof MapValue))
        {
            throw new InternalExpressionException("`requires` field must be a map of mod dependencies or a function to be executed");
        }
        Map<Value, Value> requirements = ((MapValue)reqs).getMap();
        for (Entry<Value, Value> requirement : requirements.entrySet())
        {
            boolean successful = false;
            String requiredModId = requirement.getKey().getString();
            String requirementString = requirement.getValue().getString();
            
            ModContainer mod = FabricLoader.getInstance().getModContainer(requiredModId).orElse(null);
            if (mod != null)
            {
                if (requirementString.equals("*"))
                    continue;
                Version presentVersion = mod.getMetadata().getVersion();
                BiPredicate<SemanticVersion, SemanticVersion> tester = null;
                String remainingRequirementString = requirementString;
                for (Entry<String, BiPredicate<SemanticVersion, SemanticVersion>> s : VERSION_PREFIXES.entrySet())
                {
                    if (requirementString.startsWith(s.getKey()))
                    {
                        tester = s.getValue();
                        remainingRequirementString = requirementString.substring(s.getKey().length());
                    }
                }
                if (tester == null)
                    tester = VERSION_PREFIXES.get("=");
                if (presentVersion instanceof SemanticVersion)
                {
                    SemanticVersion requiredVersion = null;
                    try {
                        requiredVersion = SemanticVersion.parse(remainingRequirementString);
                    }
                    catch (VersionParsingException e)
                    {
                        throw new InternalExpressionException("Failed to parse semantic version for '" + requiredModId + "' in 'requires': " + e.getMessage());
                    }
                    if (tester.test(requiredVersion, (SemanticVersion) presentVersion))
                        successful = true;
                }
                else
                {
                    if (FabricLoader.getInstance().isDevelopmentEnvironment())
                        successful = true; // Autoversioning breaks semver in dev (loads as ${version})
                    else
                    {
                        if (!(tester == VERSION_PREFIXES.get("=")))
                            throw new InternalExpressionException("Mod '"+requiredModId+"' doesn't use semversion, thus can only check version equality");
                        if (presentVersion.getFriendlyString().equals(remainingRequirementString))
                            successful = true;
                    }
                }
            }
            if (!successful)
            {
                throw new InternalExpressionException(getName()+" requires " + requiredModId + " version " + requirementString + " in order to load");
            }
        }
    }

    private Boolean addLegacyCommand(Consumer<Text> notifier)
    {
        if (main == null) return false;
        if (getFunction("__command") == null) return false;

        if (scriptServer.isInvalidCommandRoot(getName()))
        {
            notifier.accept(Messenger.c("gi Tried to mask vanilla command."));
            return null;
        }

        Predicate<ServerCommandSource> configValidator;
        try
        {
            configValidator = getCommandConfigPermissions();
        }
        catch (CommandSyntaxException e)
        {
            notifier.accept(Messenger.c("rb "+e.getMessage()));
            return null;
        }
        String hostName = getName();
        LiteralArgumentBuilder<ServerCommandSource> command = literal(hostName).
                requires((player) -> scriptServer.modules.containsKey(hostName) && commandValidator.test(player) && configValidator.test(player)).
                executes( (c) ->
                {
                    CarpetScriptHost targetHost = scriptServer.modules.get(hostName).retrieveOwnForExecution(c.getSource());
                    Value response = targetHost.handleCommandLegacy(c.getSource(),"__command", null, "");
                    if (!response.isNull()) Messenger.m(c.getSource(), "gi "+response.getString());
                    return (int)response.readInteger();
                });

        boolean hasTypeSupport = appConfig.getOrDefault(StringValue.of("legacy_command_type_support"), Value.FALSE).getBoolean();

        for (String function : globalFunctionNames(main, s ->  !s.startsWith("_")).sorted().collect(Collectors.toList()))
        {
            if (hasTypeSupport)
            {
                try
                {
                    FunctionValue functionValue = getFunction(function);
                    command = addPathToCommand(
                            command,
                            CommandToken.parseSpec(CommandToken.specFromSignature(functionValue), this),
                            FunctionArgument.fromCommandSpec(this, functionValue)
                    );
                }
                catch (CommandSyntaxException e)
                {
                    return false;
                }
            }
            else
            {
                command = command.
                        then(literal(function).
                                requires((player) -> scriptServer.modules.containsKey(hostName) && scriptServer.modules.get(hostName).getFunction(function) != null).
                                executes((c) -> {
                                    CarpetScriptHost targetHost = scriptServer.modules.get(hostName).retrieveOwnForExecution(c.getSource());
                                    Value response = targetHost.handleCommandLegacy(c.getSource(),function, null, "");
                                    if (!response.isNull()) Messenger.m(c.getSource(), "gi " + response.getString());
                                    return (int) response.readInteger();
                                }).
                                then(argument("args...", StringArgumentType.greedyString()).
                                        executes( (c) -> {
                                            CarpetScriptHost targetHost = scriptServer.modules.get(hostName).retrieveOwnForExecution(c.getSource());
                                            Value response = targetHost.handleCommandLegacy(c.getSource(),function, null, StringArgumentType.getString(c, "args..."));
                                            if (!response.isNull()) Messenger.m(c.getSource(), "gi "+response.getString());
                                            return (int)response.readInteger();
                                        })));
            }
        }
        scriptServer.server.getCommandManager().getDispatcher().register(command);
        return true;
    }

    public LiteralArgumentBuilder<ServerCommandSource> readCommands(Predicate<ServerCommandSource> useValidator) throws CommandSyntaxException
    {
        Value commands = appConfig.get(StringValue.of("commands"));

        if (commands == null) return null;
        if (!(commands instanceof MapValue))
            throw CommandArgument.error("'commands' element in config should be a map");
        List<Pair<List<CommandToken>,FunctionArgument>> commandEntries = new ArrayList<>();

        for (Map.Entry<Value, Value> commandsData : ((MapValue)commands).getMap().entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toList()))
        {
            List<CommandToken> elements = CommandToken.parseSpec(commandsData.getKey().getString(), this);
            FunctionArgument funSpec = FunctionArgument.fromCommandSpec(this, commandsData.getValue());
            commandEntries.add(Pair.of(elements, funSpec));
        }
        commandEntries.sort(new ListComparator<>());
        if (!appConfig.getOrDefault(StringValue.of("allow_command_conflicts"), Value.FALSE).getBoolean())
        {
            for (int i = 0; i < commandEntries.size()-1; i++)
            {
                List<CommandToken> first = commandEntries.get(i).getKey();
                List<CommandToken> other = commandEntries.get(i+1).getKey();
                int checkSize = Math.min(first.size(), other.size());
                for (int t = 0; t < checkSize; t++)
                {
                    CommandToken tik = first.get(t);
                    CommandToken tok = other.get(t);
                    if (tik.isArgument && tok.isArgument && !tik.surface.equals(tok.surface))
                        throw CommandArgument.error("Conflicting commands: \n"+
                                " - [" +first.stream().map(tt -> tt.surface).collect(Collectors.joining(" ")) + "] at "+tik.surface+"\n"+
                                " - [" +other.stream().map(tt -> tt.surface).collect(Collectors.joining(" ")) + "] at "+tok.surface+"\n");
                    if (!tik.equals(tok))
                        break;
                }
            }
        }
        return this.getNewCommandTree(commandEntries, useValidator);
    }

    @Override
    protected Module getModuleOrLibraryByName(String name)
    {
        Module module = scriptServer.getModule(name, true);
        if (module == null || module.getCode() == null)
            throw new InternalExpressionException("Unable to locate package: "+name);
        return module;
    }

    @Override
    protected void runModuleCode(Context c, Module module)
    {
        CarpetContext cc = (CarpetContext)c;
        CarpetExpression ex = new CarpetExpression(module, module.getCode(), cc.s, cc.origin);
        ex.getExpr().asATextSource();
        ex.scriptRunCommand(this, cc.origin);
    }

    @Override
    public void delFunction(Module module, String funName)
    {
        super.delFunction(module, funName);
        // mcarpet
        if (funName.startsWith("__on_"))
        {
            // this is nasty, we have the host and function, yet we add it via names, but hey - works for now
            String event = funName.replaceFirst("__on_","");
            scriptServer.events.removeBuiltInEvent(event, this, funName);
        }
    }

    public List<CarpetScriptHost> retrieveForExecution(ServerCommandSource source, String optionalTarget)
    {
        List<CarpetScriptHost> targets = new ArrayList<>();
        if (perUser)
        {
            if (optionalTarget == null)
            {
                for (ServerPlayerEntity player : source.getMinecraftServer().getPlayerManager().getPlayerList())
                {
                    CarpetScriptHost host = (CarpetScriptHost) retrieveForExecution(player.getEntityName());
                    targets.add(host);
                    if (host.errorSnooper == null) host.setChatErrorSnooper(player.getCommandSource());
                }
            }
            else
            {
                ServerPlayerEntity player = source.getMinecraftServer().getPlayerManager().getPlayer(optionalTarget);
                if (player != null)
                {
                    CarpetScriptHost host = (CarpetScriptHost) retrieveForExecution(player.getEntityName());
                    targets.add(host);
                    if (host.errorSnooper == null) host.setChatErrorSnooper(player.getCommandSource());
                }
            }
        }
        else
        {
            targets.add(this);
            if (this.errorSnooper == null) this.setChatErrorSnooper(source);
        }
        return targets;
    }

    public CarpetScriptHost retrieveOwnForExecution(ServerCommandSource source) throws CommandSyntaxException
    {
        if (!perUser)
        {
            if (errorSnooper == null) setChatErrorSnooper(source);
            return this;
        }
        // user based
        ServerPlayerEntity player;
        try
        {
            player = source.getPlayer();
        }
        catch (CommandSyntaxException ignored)
        {
            throw new SimpleCommandExceptionType(new LiteralText("Cannot run player based apps without the player context")).create();
        }
        CarpetScriptHost userHost = (CarpetScriptHost)retrieveForExecution(player.getEntityName());
        if (userHost.errorSnooper == null) userHost.setChatErrorSnooper(source);
        return userHost;
    }

    public Value handleCommandLegacy(ServerCommandSource source, String call, List<Integer> coords, String arg)
    {
        try
        {
            CarpetProfiler.ProfilerToken currentSection = CarpetProfiler.start_section(null, "Scarpet command", CarpetProfiler.TYPE.GENERAL);
            Value res = callLegacy(source, call, coords, arg);
            CarpetProfiler.end_current_section(currentSection);
            return res;
        }
        catch (CarpetExpressionException exc)
        {
            handleErrorWithStack("Error while running custom command", exc);
            return Value.NULL;
        }
        catch (ArithmeticException ae)
        {
            handleErrorWithStack("Math doesn't compute", ae);
            return Value.NULL;
        }
    }

    public Value handleCommand(ServerCommandSource source, FunctionValue function, List<Value> args)
    {
        try
        {
            return call(source, function, args);
        }
        catch (CarpetExpressionException exc)
        {
            handleErrorWithStack("Error while running custom command", exc);
            return Value.NULL;
        }
        catch (ArithmeticException ae)
        {
            handleErrorWithStack("Math doesn't compute", ae);
            return Value.NULL;
        }
    }

    public Value callLegacy(ServerCommandSource source, String call, List<Integer> coords, String arg)
    {
        if (CarpetServer.scriptServer.stopAll)
            throw new CarpetExpressionException("SCARPET PAUSED", null);
        FunctionValue function = getFunction(call);
        if (function == null)
            throw new CarpetExpressionException("UNDEFINED", null);
        List<LazyValue> argv = new ArrayList<>();
        if (coords != null)
            for (Integer i: coords)
                argv.add( (c, t) -> new NumericValue(i));
        String sign = "";
        for (Tokenizer.Token tok : Tokenizer.simplepass(arg))
        {
            switch (tok.type)
            {
                case VARIABLE:
                    LazyValue var = getGlobalVariable(tok.surface);
                    if (var != null)
                    {
                        argv.add(var);
                        break;
                    }
                case STRINGPARAM:
                    argv.add((c, t) -> new StringValue(tok.surface));
                    sign = "";
                    break;

                case LITERAL:
                    try
                    {
                        String finalSign = sign;
                        argv.add((c, t) ->new NumericValue(finalSign+tok.surface));
                        sign = "";
                    }
                    catch (NumberFormatException exception)
                    {
                        throw new CarpetExpressionException("Fail: "+sign+tok.surface+" seems like a number but it is" +
                                " not a number. Use quotes to ensure its a string", null);
                    }
                    break;
                case HEX_LITERAL:
                    try
                    {
                        String finalSign = sign;
                        argv.add((c, t) -> new NumericValue(new BigInteger(finalSign+tok.surface.substring(2), 16).doubleValue()));
                        sign = "";
                    }
                    catch (NumberFormatException exception)
                    {
                        throw new CarpetExpressionException("Fail: "+sign+tok.surface+" seems like a number but it is" +
                                " not a number. Use quotes to ensure its a string", null);
                    }
                    break;
                case OPERATOR:
                case UNARY_OPERATOR:
                    if ((tok.surface.equals("-") || tok.surface.equals("-u")) && sign.isEmpty())
                    {
                        sign = "-";
                    }
                    else
                    {
                        throw new CarpetExpressionException("Fail: operators, like " + tok.surface + " are not " +
                                "allowed in invoke", null);
                    }
                    break;
                case FUNCTION:
                    throw new CarpetExpressionException("Fail: passing functions like "+tok.surface+"() to invoke is " +
                            "not allowed", null);
                case OPEN_PAREN:
                case COMMA:
                case CLOSE_PAREN:
                case MARKER:
                    throw new CarpetExpressionException("Fail: "+tok.surface+" is not allowed in invoke", null);
            }
        }
        List<String> args = function.getArguments();
        if (argv.size() != args.size())
        {
            String error = "Fail: stored function "+call+" takes "+args.size()+" arguments, not "+argv.size()+ ":\n";
            for (int i = 0; i < max(argv.size(), args.size()); i++)
            {
                error += (i<args.size()?args.get(i):"??")+" => "+(i<argv.size()?argv.get(i).evalValue(null).getString():"??")+"\n";
            }
            throw new CarpetExpressionException(error, null);
        }
        try
        {
            // TODO: this is just for now - invoke would be able to invoke other hosts scripts
            Context context = new CarpetContext(this, source, BlockPos.ORIGIN);
            return function.getExpression().evalValue(
                    () -> function.lazyEval(context, Context.VOID, function.getExpression(), function.getToken(), argv),
                    context,
                    Context.VOID
            );
        }
        catch (ExpressionException e)
        {
            throw new CarpetExpressionException(e.getMessage(), e.stack);
        }
    }

    public Value call(ServerCommandSource source, FunctionValue function, List<Value> argv)
    {
        if (CarpetServer.scriptServer.stopAll)
            throw new CarpetExpressionException("SCARPET PAUSED", null);

        List<String> args = function.getArguments();
        if (argv.size() != args.size())
        {
            String error = "Fail: stored function "+function.getPrettyString()+" takes "+args.size()+" arguments, not "+argv.size()+ ":\n";
            for (int i = 0; i < max(argv.size(), args.size()); i++)
            {
                error += (i<args.size()?args.get(i):"??")+" => "+(i<argv.size()?argv.get(i).getString():"??")+"\n";
            }
            throw new CarpetExpressionException(error, null);
        }
        try
        {
            // TODO: this is just for now - invoke would be able to invoke other hosts scripts
            Context context = new CarpetContext(this, source, BlockPos.ORIGIN);
            return function.getExpression().evalValue(
                    () -> function.execute(context, Context.VOID, function.getExpression(), function.getToken(), argv),
                    context,
                    Context.VOID
            );
        }
        catch (ExpressionException e)
        {
            throw new CarpetExpressionException(e.getMessage(), e.stack);
        }
    }

    public Value callUDF(BlockPos pos, ServerCommandSource source, FunctionValue fun, List<Value> argv) throws InvalidCallbackException
    {
        if (CarpetServer.scriptServer.stopAll)
            return Value.NULL;
        try { // cause we can't throw checked exceptions in lambda. Left if be until need to handle these more gracefully
            fun.assertArgsOk(argv, (b) -> {
                throw new InternalExpressionException("");
            });
        }
        catch (InternalExpressionException ignored)
        {
            throw new InvalidCallbackException();
        }
        try
        {
            // TODO: this is just for now - invoke would be able to invoke other hosts scripts
            Context context = new CarpetContext(this, source, pos);
            return fun.getExpression().evalValue(
                    () -> fun.execute(context, Context.VOID, fun.getExpression(), fun.getToken(), argv),
                    context,
                    Context.VOID);
        }
        catch (ExpressionException e)
        {
            handleExpressionException("Callback failed", e);
        }
        return Value.NULL;
    }

    public Value callNow(FunctionValue fun, List<Value> arguments)
    {
        ServerPlayerEntity player = (user==null)?null:scriptServer.server.getPlayerManager().getPlayer(user);
        ServerCommandSource source = (player != null)?player.getCommandSource():scriptServer.server.getCommandSource();
        try
        {
            return callUDF(BlockPos.ORIGIN, source, fun, arguments);
        }
        catch (InvalidCallbackException ignored)
        {
            return Value.NULL;
        }
    }


    @Override
    public void onClose()
    {
        super.onClose();
        FunctionValue closing = getFunction("__on_close");
        if (closing != null && (parent != null || !isPerUser()))
            // either global instance of a global task, or
            // user host in player scoped app
        {
            callNow(closing, Collections.emptyList());
        }
        if (user == null)
        {

            String markerName = Auxiliary.MARKER_STRING + "_" + ((getName() == null) ? "" : getName());
            for (ServerWorld world : scriptServer.server.getWorlds())
            {
                for (Entity e : world.getEntitiesByType(EntityType.ARMOR_STAND, (as) -> as.getScoreboardTags().contains(markerName)))
                {
                    e.remove();
                }
            }
            if (this.saveTimeout > 0)
                dumpState();
        }
    }

    private void dumpState()
    {
        Module.saveData(main, globalState);
    }

    private Tag loadState()
    {
        return Module.getData(main);
    }

    public Tag readFileTag(FileArgument fdesc)
    {
        if (getName() == null && !fdesc.isShared) return null;
        if (fdesc.resource != null)
            return fdesc.getNbtData(main);
        if (parent == null)
            return globalState;
        return ((CarpetScriptHost)parent).globalState;
    }

    public boolean writeTagFile(Tag tag, FileArgument fdesc)
    {
        if (getName() == null && !fdesc.isShared) return false; // if belongs to an app, cannot be default host.

        if (fdesc.resource != null)
        {
            return fdesc.saveNbtData(main, tag);
        }

        CarpetScriptHost responsibleHost = (parent != null)?(CarpetScriptHost) parent:this;
        responsibleHost.globalState = tag;
        if (responsibleHost.saveTimeout == 0)
        {
            responsibleHost.dumpState();
            responsibleHost.saveTimeout = 200;
        }
        return true;
    }

    public boolean removeResourceFile(FileArgument fdesc)
    {
        if (getName() == null && !fdesc.isShared) return false; //
        return fdesc.dropExistingFile(main);
    }

    public boolean appendLogFile(FileArgument fdesc, List<String> data)
    {
        if (getName() == null && !fdesc.isShared) return false; // if belongs to an app, cannot be default host.
        return fdesc.appendToTextFile(main, data);
    }

    public List<String> readTextResource(FileArgument fdesc)
    {
        if (getName() == null && !fdesc.isShared) return null;
        return fdesc.listFile(main);
    }
    
    public JsonElement readJsonFile(FileArgument fdesc)
    {
        if (getName() == null && !fdesc.isShared) return null;
        return fdesc.readJsonFile(main);
    }

    public Stream<String> listFolder(FileArgument fdesc)
    {
        if (getName() == null && !fdesc.isShared) return null; //
        return fdesc.listFolder(main);
    }


    public void tick()
    {
        if (this.saveTimeout > 0)
        {
            this.saveTimeout --;
            if (this.saveTimeout == 0)
            {
                dumpState();
            }
        }
    }

    public void setChatErrorSnooper(ServerCommandSource source)
    {
        responsibleSource = source;
        errorSnooper = (expr, /*Nullable*/ token, message) ->
        {
            try
            {
                source.getPlayer();
            }
            catch (CommandSyntaxException e)
            {
                return null;
            }

            String shebang = message+" in "+expr.getModuleName();
            if (token != null)
            {
                String[] lines = expr.getCodeString().split("\n");

                if (lines.length > 1)
                {
                    shebang += " at line " + (token.lineno + 1) + ", pos " + (token.linepos + 1);
                }
                else
                {
                    shebang += " at pos " + (token.pos + 1);
                }
                Messenger.m(source, "r " + shebang);
                if (lines.length > 1 && token.lineno > 0)
                {
                    Messenger.m(source, "l " + lines[token.lineno - 1]);
                }
                Messenger.m(source, "l " + lines[token.lineno].substring(0, token.linepos), "r  HERE>> ", "l " +
                        lines[token.lineno].substring(token.linepos));
                if (lines.length > 1 && token.lineno < lines.length - 1)
                {
                    Messenger.m(source, "l " + lines[token.lineno + 1]);
                }
            }
            else
            {
                Messenger.m(source, "r " + shebang);
            }
            return new ArrayList<>();
        };
    }

    @Override
    public void resetErrorSnooper()
    {
        responsibleSource = null;
        super.resetErrorSnooper();
    }

    public void handleErrorWithStack(String intro, Exception exception)
    {
        if (responsibleSource != null)
        {
            if (exception instanceof CarpetExpressionException) ((CarpetExpressionException) exception).printStack(responsibleSource);
            String message = exception.getMessage();
            Messenger.m(responsibleSource, "r "+intro+(message.isEmpty()?"":": "+message));
        }
        else
        {
            CarpetSettings.LOG.error(intro+": "+exception.getMessage());
        }
    }

    @Override
    synchronized public void handleExpressionException(String message, ExpressionException exc)
    {
        handleErrorWithStack(message, new CarpetExpressionException(exc.getMessage(), exc.stack));
    }

    public CarpetScriptServer getScriptServer()
    {
        return scriptServer;
    }

    @Override
    public boolean issueDeprecation(String feature)
    {
        if(super.issueDeprecation(feature))
        {
            Messenger.m(responsibleSource, "rb '"+feature+"' is deprecated and soon will be removed. Please consult the docs for their replacement");
            return true;
        }
        return false;
    }
}
