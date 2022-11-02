package carpet.script;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.script.api.Auxiliary;
import carpet.script.argument.FileArgument;
import carpet.script.argument.FunctionArgument;
import carpet.script.command.CommandArgument;
import carpet.script.command.CommandToken;
import carpet.script.exception.CarpetExpressionException;
import carpet.script.exception.ExpressionException;
import carpet.script.exception.IntegrityException;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.InvalidCallbackException;
import carpet.script.exception.LoadException;
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
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.apache.commons.lang3.tuple.Pair;

import java.math.BigInteger;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Math.max;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class CarpetScriptHost extends ScriptHost
{
    public CommandSourceStack responsibleSource;

    private Tag globalState;
    private int saveTimeout;
    public boolean persistenceRequired;
    public double eventPriority;

    public Map<Value, Value> appConfig;
    public Map<String, CommandArgument> appArgTypes;

    Predicate<CommandSourceStack> commandValidator;
    boolean isRuleApp;
    public AppStoreManager.StoreNode storeSource;

    private CarpetScriptHost(CarpetScriptServer server, Module code, boolean perUser, ScriptHost parent, Map<Value, Value> config, Map<String, CommandArgument> argTypes, Predicate<CommandSourceStack> commandValidator, boolean isRuleApp)
    {
        super(code, server, perUser, parent);
        this.saveTimeout = 0;
        persistenceRequired = true;
        if (parent == null && code != null) // app, not a global host
        {
            globalState = loadState();
        }
        else if (parent != null)
        {
            persistenceRequired = ((CarpetScriptHost)parent).persistenceRequired;
            strict = parent.strict;
        }
        appConfig = config;
        appArgTypes = argTypes;
        this.commandValidator = commandValidator;
        this.isRuleApp = isRuleApp;
        storeSource = null;
    }

    public static CarpetScriptHost create(CarpetScriptServer scriptServer, Module module, boolean perPlayer, CommandSourceStack source, Predicate<CommandSourceStack> commandValidator, boolean isRuleApp, AppStoreManager.StoreNode storeSource)
    {
        CarpetScriptHost host = new CarpetScriptHost(scriptServer, module, perPlayer, null, Collections.emptyMap(), new HashMap<>(), commandValidator, isRuleApp);
        // parse code and convert to expression
        if (module != null)
        {
            try
            {
                host.setChatErrorSnooper(source);
                CarpetExpression ex = new CarpetExpression(host.main, module.code(), source, new BlockPos(0, 0, 0));
                ex.getExpr().asATextSource();
                host.storeSource = storeSource;
                ex.scriptRunCommand(host, new BlockPos(source.getPosition()));
            }
            catch (CarpetExpressionException e)
            {
                host.handleErrorWithStack("Error while evaluating expression", e);
                throw new LoadException();
            }
            catch (ArithmeticException ae) // is this branch ever reached? Seems like arithmetic exceptions are converted to CEEs earlier
            {
                host.handleErrorWithStack("Math doesn't compute", ae);
                throw new LoadException();
            }
            catch (StackOverflowError soe)
            {
                host.handleErrorWithStack("Your thoughts are too deep", soe);
            }
            finally
            {
                host.storeSource = null;
            }
        }
        return host;
    }

    private static int execute(CommandContext<CommandSourceStack> ctx, String hostName, FunctionArgument funcSpec, List<String> paramNames) throws CommandSyntaxException
    {
        CarpetProfiler.ProfilerToken currentSection = CarpetProfiler.start_section(null, "Scarpet command", CarpetProfiler.TYPE.GENERAL);
        CarpetScriptHost cHost = CarpetServer.scriptServer.modules.get(hostName).retrieveOwnForExecution(ctx.getSource());
        List<String> argNames = funcSpec.function.getArguments();
        if ((argNames.size()-funcSpec.args.size()) != paramNames.size())
            throw new SimpleCommandExceptionType(Component.literal("Target function "+funcSpec.function.getPrettyString()+" as wrong number of arguments, required "+paramNames.size()+", found "+argNames.size()+" with "+funcSpec.args.size()+" provided")).create();
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

    public LiteralArgumentBuilder<CommandSourceStack> addPathToCommand(
            LiteralArgumentBuilder<CommandSourceStack> command,
            List<CommandToken> path,
            FunctionArgument functionSpec
    ) throws CommandSyntaxException
    {
        String hostName = main.name();
        List<String> commandArgs = path.stream().filter(t -> t.isArgument).map(t -> t.surface).collect(Collectors.toList());
        if (commandArgs.size() != (functionSpec.function.getNumParams()-functionSpec.args.size()) )
            throw CommandArgument.error("Number of parameters in function "+functionSpec.function.fullName()+" doesn't match parameters for a command");
        if (path.isEmpty())
        {
            return command.executes((c) -> execute(c, hostName, functionSpec, Collections.emptyList()));
        }
        List<CommandToken> reversedPath = new ArrayList<>(path);
        Collections.reverse(reversedPath);
        ArgumentBuilder<CommandSourceStack, ?> argChain = reversedPath.get(0).getCommandNode(this).executes(c -> execute(c, hostName, functionSpec, commandArgs));
        for (int i = 1; i < reversedPath.size(); i++)
        {
            argChain = reversedPath.get(i).getCommandNode(this).then(argChain);
        }
        return command.then(argChain);
    }

    public LiteralArgumentBuilder<CommandSourceStack> getNewCommandTree(
            List<Pair<List<CommandToken>,FunctionArgument>> entries, Predicate<CommandSourceStack> useValidator
    ) throws CommandSyntaxException
    {
        String hostName = main.name();
        Predicate<CommandSourceStack> configValidator = getCommandConfigPermissions();
        LiteralArgumentBuilder<CommandSourceStack> command = literal(hostName).
               requires((player) -> useValidator.test(player) && configValidator.test(player));
        for (Pair<List<CommandToken>,FunctionArgument> commandData : entries)
        {
            command = this.addPathToCommand(command, commandData.getKey(), commandData.getValue());
        }
        return command;
    }

    public Predicate<CommandSourceStack> getCommandConfigPermissions() throws CommandSyntaxException
    {
        Value confValue = appConfig.get(StringValue.of("command_permission"));
        if (confValue == null) return s -> true;
        if (confValue instanceof NumericValue)
        {
            int level = ((NumericValue) confValue).getInt();
            if (level < 1 || level > 4) throw CommandArgument.error("Numeric permission level for custom commands should be between 1 and 4");
            return s -> s.hasPermission(level);
        }
        if (!(confValue instanceof FunctionValue))
        {
            String perm = confValue.getString().toLowerCase(Locale.ROOT);
            switch (perm)
            {
                case "ops": return s -> s.hasPermission(2);
                case "server": return s -> !(s.getEntity() instanceof ServerPlayer);
                case "players": return s -> s.getEntity() instanceof ServerPlayer;
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
                        (s.getEntity() instanceof ServerPlayer)?new EntityValue(s.getEntity()):Value.NULL)
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
        return new CarpetScriptHost(scriptServer(), main, false, this, appConfig, appArgTypes, commandValidator, isRuleApp);
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
                    scriptServer().events.addBuiltInEvent(event, this, function, null);
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
            strict = config.getOrDefault(StringValue.of("strict"), Value.FALSE).getBoolean();
            eventPriority = config.getOrDefault(new StringValue("event_priority"), Value.ZERO).readDoubleNumber();
            // check requires
            Value loadRequirements = config.get(new StringValue("requires"));
            if (loadRequirements instanceof FunctionValue)
            {
                Value reqResult = callNow((FunctionValue) loadRequirements, Collections.emptyList());
                if (reqResult.getBoolean()) // != false or null
                    throw new LoadException(reqResult.getString());
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
                Value libraries = config.get(new StringValue("libraries"));
                if (libraries != null)
                {
                    if (!(libraries instanceof ListValue)) throw new InternalExpressionException("App libraries not defined as a list");
                    for (Value library : ((ListValue) libraries).getItems())
                    {
                        AppStoreManager.addLibrary(this, storeSource, library);
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

    public Boolean addAppCommands(Consumer<Component> notifier)
    {
        try
        {
            readCustomArgumentTypes();
        }
        catch (CommandSyntaxException e)
        {
            notifier.accept(Messenger.c("r Error when handling of setting up custom argument types: "+e.getMessage()));
            return false;
        }
        if (appConfig.get(StringValue.of("commands")) != null)
        {
            if (scriptServer().isInvalidCommandRoot(getName()))
            {
                notifier.accept(Messenger.c("g A command with the app's name already exists in vanilla or an installed mod."));
                return null;
            }
            try
            {
                LiteralArgumentBuilder<CommandSourceStack> command = readCommands(commandValidator);
                if (command != null)
                {
                    scriptServer().server.getCommands().getDispatcher().register(command);
                    return true;
                }
                else {
                    return false;
                }
            }
            catch (CommandSyntaxException cse)
            {
                // failed
                notifier.accept(Messenger.c("r Failed to build command system: ", cse.getRawMessage()));
                return null;
            }

        }
        return addLegacyCommand(notifier);
    }
    
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
            String requiredModId = requirement.getKey().getString();
            String stringPredicate = requirement.getValue().getString();
            VersionPredicate predicate;
            try {
                predicate = VersionPredicate.parse(stringPredicate);
            } catch (VersionParsingException e) {
                throw new InternalExpressionException("Failed to parse version conditions for '" + requiredModId + "' in 'requires': " + e.getMessage());
            }

            ModContainer mod = FabricLoader.getInstance().getModContainer(requiredModId).orElse(null);
            if (mod != null)
            {
                Version presentVersion = mod.getMetadata().getVersion();
                if (predicate.test(presentVersion) || (FabricLoader.getInstance().isDevelopmentEnvironment() && !(presentVersion instanceof SemanticVersion)))
                { // in a dev env, mod version is usually replaced with ${version}, and that isn't semantic
                    continue;
                }
            }
            throw new LoadException(String.format("%s requires a version of mod '%s' matching '%s', which is missing!", getName(), requiredModId, stringPredicate));
        }
    }

    private Boolean addLegacyCommand(Consumer<Component> notifier)
    {
        if (main == null) return false;
        if (getFunction("__command") == null) return false;

        if (scriptServer().isInvalidCommandRoot(getName()))
        {
            notifier.accept(Messenger.c("g A command with the app's name already exists in vanilla or an installed mod."));
            return null;
        }

        Predicate<CommandSourceStack> configValidator;
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
        LiteralArgumentBuilder<CommandSourceStack> command = literal(hostName).
                requires((player) -> commandValidator.test(player) && configValidator.test(player)).
                executes( (c) ->
                {
                    CarpetScriptHost targetHost = scriptServer().modules.get(hostName).retrieveOwnForExecution(c.getSource());
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
                                requires((player) -> scriptServer().modules.get(hostName).getFunction(function) != null).
                                executes((c) -> {
                                    CarpetScriptHost targetHost = scriptServer().modules.get(hostName).retrieveOwnForExecution(c.getSource());
                                    Value response = targetHost.handleCommandLegacy(c.getSource(),function, null, "");
                                    if (!response.isNull()) Messenger.m(c.getSource(), "gi " + response.getString());
                                    return (int) response.readInteger();
                                }).
                                then(argument("args...", StringArgumentType.greedyString()).
                                        executes( (c) -> {
                                            CarpetScriptHost targetHost = scriptServer().modules.get(hostName).retrieveOwnForExecution(c.getSource());
                                            Value response = targetHost.handleCommandLegacy(c.getSource(),function, null, StringArgumentType.getString(c, "args..."));
                                            if (!response.isNull()) Messenger.m(c.getSource(), "gi "+response.getString());
                                            return (int)response.readInteger();
                                        })));
            }
        }
        scriptServer().server.getCommands().getDispatcher().register(command);
        return true;
    }

    public LiteralArgumentBuilder<CommandSourceStack> readCommands(Predicate<CommandSourceStack> useValidator) throws CommandSyntaxException
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
        Module module = scriptServer().getModule(name, true);
        if (module == null)
            throw new InternalExpressionException("Unable to locate package: "+name);
        return module;
    }

    @Override
    protected void runModuleCode(Context c, Module module)
    {
        CarpetContext cc = (CarpetContext)c;
        CarpetExpression ex = new CarpetExpression(module, module.code(), cc.s, cc.origin);
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
            scriptServer().events.removeBuiltInEvent(event, this, funName);
        }
    }

    public CarpetScriptHost retrieveForExecution(CommandSourceStack source, ServerPlayer player)
    {
        CarpetScriptHost target = null;
        if (!perUser)
        {
            target = this;
        }
        else if (player != null)
        {
            target = (CarpetScriptHost) retrieveForExecution(player.getScoreboardName());
        }
        if (target != null && target.errorSnooper == null) target.setChatErrorSnooper(source);
        return target;
    }

    public CarpetScriptHost retrieveOwnForExecution(CommandSourceStack source) throws CommandSyntaxException
    {
        if (!perUser)
        {
            if (errorSnooper == null) setChatErrorSnooper(source);
            return this;
        }
        // user based
        ServerPlayer player;
        try
        {
            player = source.getPlayerOrException();
        }
        catch (CommandSyntaxException ignored)
        {
            throw new SimpleCommandExceptionType(Component.literal("Cannot run player based apps without the player context")).create();
        }
        CarpetScriptHost userHost = (CarpetScriptHost)retrieveForExecution(player.getScoreboardName());
        if (userHost.errorSnooper == null) userHost.setChatErrorSnooper(source);
        return userHost;
    }

    public Value handleCommandLegacy(CommandSourceStack source, String call, List<Integer> coords, String arg)
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
        }
        catch (ArithmeticException ae)
        {
            handleErrorWithStack("Math doesn't compute", ae);
        }
        catch (StackOverflowError soe)
        {
            handleErrorWithStack("Your thoughts are too deep", soe);
        }
        return Value.NULL;
    }

    public Value handleCommand(CommandSourceStack source, FunctionValue function, List<Value> args)
    {
        try
        {
            return scriptServer().events.handleEvents.getWhileDisabled(() -> call(source, function, args));
        }
        catch (CarpetExpressionException exc)
        {
            handleErrorWithStack("Error while running custom command", exc);
        }
        catch (ArithmeticException ae)
        {
            handleErrorWithStack("Math doesn't compute", ae);
        }
        catch (StackOverflowError soe)
        {
            handleErrorWithStack("Your thoughts are too deep", soe);
        }
        return Value.NULL;
    }

    public Value callLegacy(CommandSourceStack source, String call, List<Integer> coords, String arg)
    {
        if (CarpetServer.scriptServer.stopAll)
            throw new CarpetExpressionException("SCARPET PAUSED (unpause with /script resume)", null);
        FunctionValue function = getFunction(call);
        if (function == null)
            throw new CarpetExpressionException("Couldn't find function '" + call + "' in app '" + this.getName() + "'", null);
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
                    if (var != null) argv.add(var);
                    break;
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
            assertAppIntegrity(function.getModule());
            Context context = new CarpetContext(this, source);
            return scriptServer().events.handleEvents.getWhileDisabled(() -> function.getExpression().evalValue(
                    () -> function.lazyEval(context, Context.VOID, function.getExpression(), function.getToken(), argv),
                    context,
                    Context.VOID
            ));
        }
        catch (ExpressionException e)
        {
            throw new CarpetExpressionException(e.getMessage(), e.stack);
        }
    }

    public Value call(CommandSourceStack source, FunctionValue function, List<Value> argv)
    {
        if (CarpetServer.scriptServer.stopAll)
            throw new CarpetExpressionException("SCARPET PAUSED (unpause with /script resume)", null);

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
            assertAppIntegrity(function.getModule());
            Context context = new CarpetContext(this, source);
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

    public Value callUDF(CommandSourceStack source, FunctionValue fun, List<Value> argv) throws InvalidCallbackException, IntegrityException
    {
        return callUDF(BlockPos.ZERO, source, fun, argv);
    }

    public Value callUDF(BlockPos origin, CommandSourceStack source, FunctionValue fun, List<Value> argv) throws InvalidCallbackException, IntegrityException
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
            assertAppIntegrity(fun.getModule());
            Context context = new CarpetContext(this, source, origin);
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
        ServerPlayer player = (user==null)?null:scriptServer().server.getPlayerList().getPlayerByName(user);
        CommandSourceStack source = (player != null)?player.createCommandSourceStack():scriptServer().server.createCommandSourceStack();
        return scriptServer().events.handleEvents.getWhileDisabled(()->{
        try
        {
            return callUDF(source, fun, arguments);
        }
        catch (InvalidCallbackException ignored)
        {
            return Value.NULL;
        }
        });
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
            for (ServerLevel world : scriptServer().server.getAllLevels())
            {
                for (Entity e : world.getEntities(EntityType.ARMOR_STAND, (as) -> as.getTags().contains(markerName)))
                {
                    e.discard();
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

    public boolean applyActionForResource(String path, boolean shared, Consumer<Path> action)
    {
        FileArgument fdesc = FileArgument.resourceFromPath(path, FileArgument.Reason.CREATE, shared);
        return fdesc.findPathAndApply(main, action);
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

    public void setChatErrorSnooper(CommandSourceStack source)
    {
        responsibleSource = source;
        errorSnooper = (expr, /*Nullable*/ token, ctx, message) ->
        {
            try
            {
                source.getPlayerOrException();
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
                    Messenger.m(source, withLocals("l", lines[token.lineno - 1], ctx));
                }
                Messenger.m(source, withLocals("l", lines[token.lineno].substring(0, token.linepos), ctx), "r  HERE>> ", 
                        withLocals("l", lines[token.lineno].substring(token.linepos), ctx));
                if (lines.length > 1 && token.lineno < lines.length - 1)
                {
                    Messenger.m(source, withLocals("l", lines[token.lineno + 1], ctx));
                }
            }
            else
            {
                Messenger.m(source, "r " + shebang);
            }
            return new ArrayList<>();
        };
    }

    /**
     * <p>Creates a {@link Component} using {@link Messenger} that has the locals in the {@code line} snippet with a hover over
     * tooltip with the value of the local at that location</p> 
     * @param line The line to find references to locals on
     * @param context The {@link Context} to extract the locals from
     * @param format The format to apply to each part of the line, without the trailing space
     * @return A BaseText of the given line with the given format, that is visibly the same as passing those to Messenger, but with references to the
     *            locals in the {@link Context} with a hover over tooltip text
     * @implNote The implementation of this method is far from perfect, and won't detect actual references to variables, but try to find the strings
     *              and add the hover effect to anything that equals to any variable name, so short variable names may appear on random positions
     */
    private static Component withLocals(String format, String line, Context context)
    {
        format += " ";
        List<String> stringsToFormat = new ArrayList<>();
        TreeMap<Integer, String> posToLocal = new TreeMap<>(); //Holds whether a local variable name is found at a specific index
        for (String local: context.variables.keySet())
        {
            int pos = line.indexOf(local);
            while (pos != -1)
            {
                posToLocal.merge(pos, local, (existingLocal, newLocal) ->
                {
                    if (newLocal.length() > existingLocal.length()) // Prefer longer variable names at the same position, since else single chars everywhere
                        return local;
                    else
                        return existingLocal;
                });
                pos = line.indexOf(local, pos + 1);
            }
        }
        int lastPos = 0;
        for (Entry<Integer, String> foundLocal : posToLocal.entrySet())
        {
            if (foundLocal.getKey() < lastPos) // system isn't perfect: part of another local
                continue;
            stringsToFormat.add(format + line.substring(lastPos, foundLocal.getKey()));
            stringsToFormat.add(format + foundLocal.getValue());
            Value val = context.variables.get(foundLocal.getValue()).evalValue(context);
            String type = val.getTypeString();
            String value;
            try {
                value = val.getPrettyString();
            } catch (StackOverflowError e) {
                value = "Exception while rendering variable, there seems to be a recursive reference in there";
            }
            stringsToFormat.add("^ Value of '" + foundLocal.getValue() + "' at position (" + type + "): \n"
                        + value);
            lastPos = foundLocal.getKey() + foundLocal.getValue().length();
        }
        if (line.length() != lastPos)
            stringsToFormat.add(format + line.substring(lastPos, line.length()));
        return Messenger.c(stringsToFormat.toArray());
    }

    @Override
    public void resetErrorSnooper()
    {
        responsibleSource = null;
        super.resetErrorSnooper();
    }

    public void handleErrorWithStack(String intro, Throwable exception)
    {
        if (responsibleSource != null)
        {
            if (exception instanceof CarpetExpressionException) ((CarpetExpressionException) exception).printStack(responsibleSource);
            String message = exception.getMessage();
            Messenger.m(responsibleSource, "r "+intro+( (message == null || message.isEmpty())?"":": "+message));
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

    /**
     * @deprecated Use {@link #scriptServer()} instead
     */
    @Deprecated(forRemoval = true)
    public CarpetScriptServer getScriptServer()
    {
        return scriptServer();
    }
    
    @Override
    public CarpetScriptServer scriptServer()
    {
        return (CarpetScriptServer)super.scriptServer();
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
