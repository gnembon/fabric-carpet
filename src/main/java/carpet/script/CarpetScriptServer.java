package carpet.script;

import carpet.CarpetSettings;
import carpet.script.argument.FunctionArgument;
import carpet.script.bundled.BundledModule;
import carpet.CarpetServer;
import carpet.script.bundled.FileModule;
import carpet.script.bundled.Module;
import carpet.script.command.CommandToken;
import carpet.script.exception.InvalidCallbackException;
import carpet.script.value.FunctionValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import carpet.utils.Messenger;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class CarpetScriptServer
{
    //make static for now, but will change that later:
    public final MinecraftServer server;
    public  CarpetScriptHost globalHost;
    public  Map<String, CarpetScriptHost> modules;
    public  Set<String> unloadableModules;
    public long tickStart;
    public boolean stopAll;
    private  Set<String> holyMoly;
    public  CarpetEventServer events;

    private static final List<Module> bundledModuleData = new ArrayList<>();
    private static final List<Module> ruleModuleData = new ArrayList<>();

    /**
     * Registers a Scarpet App to be always available to be loaded
     * in the /script load list.
     * @see BundledModule#fromPath(String, String, boolean)
     * 
     * @param app The {@link BundledModule} of the app
     */
    public static void registerBuiltInScript(BundledModule app)
    {
        bundledModuleData.add(app);
    }
    
    /**
     * Registers a Scarpet App to be used as a Rule App
     * (to be controlled with the value of a Carpet rule).
     * Libraries should be registered with {@link #registerBuiltInScript(BundledModule)} instead
     * @see BundledModule#fromPath(String, String, boolean)
     * 
     * @param app The {@link BundledModule} of the app.
     */
    public static void registerSettingsApp(BundledModule app) {
    	ruleModuleData.add(app);
    }

    static
    {
        registerBuiltInScript(BundledModule.carpetNative("camera", false));
        registerBuiltInScript(BundledModule.carpetNative("overlay", false));
        registerBuiltInScript(BundledModule.carpetNative("event_test", false));
        registerBuiltInScript(BundledModule.carpetNative("stats_test", false));
        registerBuiltInScript(BundledModule.carpetNative("math", true));
        registerBuiltInScript(BundledModule.carpetNative("chunk_display", false));
        registerBuiltInScript(BundledModule.carpetNative("ai_tracker", false));
        registerBuiltInScript(BundledModule.carpetNative("draw_beta", false));
        registerBuiltInScript(BundledModule.carpetNative("distance_beta", false));
    }

    public CarpetScriptServer(MinecraftServer server)
    {
        this.server = server;
        init();
    }

    private void init()
    {
        ScriptHost.systemGlobals.clear();
        events = new CarpetEventServer(server);
        modules = new HashMap<>();
        unloadableModules = new HashSet<String>();
        tickStart = 0L;
        stopAll = false;
        holyMoly = server.getCommandManager().getDispatcher().getRoot().getChildren().stream().map(CommandNode::getName).collect(Collectors.toSet());
        globalHost = CarpetScriptHost.create(this, null, false, null, p -> true, false);
    }

    public void initializeForWorld()
    {
        CarpetServer.settingsManager.initializeScarpetRules();
        CarpetServer.extensions.forEach(e -> {
            if (e.customSettingsManager() != null) {
                e.customSettingsManager().initializeScarpetRules();
            }
        });
        if (CarpetSettings.scriptsAutoload)
        {
            for (String moduleName: listAvailableModules(false))
            {
                addScriptHost(server.getCommandSource(), moduleName, null, true, true, false);
            }
        }
        CarpetEventServer.Event.START.onTick();
    }

    public Module getModule(String name, boolean allowLibraries)
    {
        try {
            Path folder = server.getSavePath(WorldSavePath.ROOT).resolve("scripts");
            Files.createDirectories(folder);
            Optional<Path>
            scriptPath = Files.list(folder)
                .filter(script -> 
                    script.getFileName().toString().equalsIgnoreCase(name+".sc") || 
                    (allowLibraries && script.getFileName().toString().equalsIgnoreCase(name+".scl"))
                ).findFirst();
            if (scriptPath.isPresent())
                return new FileModule(scriptPath.get());

            if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT)
            {
                Path globalFolder = FabricLoader.getInstance().getConfigDir().resolve("carpet/scripts");
                Files.createDirectories(globalFolder);
                scriptPath = Files.walk(globalFolder)
                        .filter(script -> script.getFileName().toString().equalsIgnoreCase(name + ".sc") ||
                                (allowLibraries && script.getFileName().toString().equalsIgnoreCase(name + ".scl")))
                        .findFirst();
                if (scriptPath.isPresent())
                    return new FileModule(scriptPath.get());
            }
        } catch (IOException e) {
            CarpetSettings.LOG.error("Exception while loading the app: ", e);
        }
        for (Module moduleData : bundledModuleData)
        {
            if (moduleData.getName().equalsIgnoreCase(name) && (allowLibraries || !moduleData.isLibrary()))
            {
                return moduleData;
            }
        }
        return null;
    }
    
    public Module getRuleModule(String name) 
    {
        for (Module moduleData : ruleModuleData)
        {
            if (moduleData.getName().equalsIgnoreCase(name))
            {
                return moduleData;
            }
        }
        return null;
    }

    public List<String> listAvailableModules(boolean includeBuiltIns)
    {
        List<String> moduleNames = new ArrayList<>();
        if (includeBuiltIns)
        {
            for (Module mi : bundledModuleData)
            {
                if (!mi.isLibrary() && !mi.getName().endsWith("_beta")) moduleNames.add(mi.getName());
            }
        }
        try {
            Path worldScripts = server.getSavePath(WorldSavePath.ROOT).resolve("scripts");
            Files.createDirectories(worldScripts);
            Files.list(worldScripts)
                .filter(f -> f.toString().endsWith(".sc"))
                .forEach(f -> moduleNames.add(f.getFileName().toString().replaceFirst("\\.sc$","").toLowerCase(Locale.ROOT)));

            if (includeBuiltIns && (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT))
            {
                Path globalScripts = FabricLoader.getInstance().getConfigDir().resolve("carpet/scripts");
                Files.createDirectories(globalScripts);
                Files.walk(globalScripts)
                    .filter(f -> f.toString().endsWith(".sc"))
                    .forEach(f -> moduleNames.add(f.getFileName().toString().replaceFirst("\\.sc$","").toLowerCase(Locale.ROOT)));
            }
        } catch (IOException e) {
            CarpetSettings.LOG.error("Exception while searching for apps: ", e);
        }
        return moduleNames;
    }

    public ScriptHost getHostByName(String name)
    {
        if (name == null)
            return globalHost;
        return modules.get(name);
    }

    public boolean addScriptHost(ServerCommandSource source, String name, Function<ServerCommandSource, Boolean> commandValidator,
                                 boolean perPlayer, boolean autoload, boolean isRuleApp)
    {
        if (commandValidator == null) commandValidator = p -> true;
        long start = System.nanoTime();
        name = name.toLowerCase(Locale.ROOT);
        boolean reload = false;
        if (modules.containsKey(name))
        {
            if (isRuleApp) return false;
            removeScriptHost(source, name, false, isRuleApp);
            reload = true;
        }
        Module module = isRuleApp ? getRuleModule(name) : getModule(name, false);
        if (module == null)
        {
            Messenger.m(source, "r Failed to add "+name+" app");
            return false;
        }
        CarpetScriptHost newHost = CarpetScriptHost.create(this, module, perPlayer, source, commandValidator, isRuleApp);
        if (newHost == null)
        {
            Messenger.m(source, "r Failed to add "+name+" app");
            return false;
        }
        // config needs to be read as we load the app since some events use that info
        //if (!newHost.readConfig())
        //{
        //    Messenger.m(source, "r Failed to add "+name+" app: invalid app config (via '__config()' function)");
        //    return false;
        //}
        if (module.getCode() == null)
        {
            Messenger.m(source, "r Unable to load "+name+" app - not found");
            return false;
        }

        modules.put(name, newHost);
        if (!isRuleApp) unloadableModules.add(name);

        if (autoload && !newHost.persistenceRequired)
        {
            removeScriptHost(source, name, false, false);
            return false;
        }
        //addEvents(source, name);
        String action = reload?"reloaded":"loaded";
        if (newHost.appConfig.get(StringValue.of("commands")) != null)
        {
            try
            {
                LiteralArgumentBuilder<ServerCommandSource> command = newHost.readCommands(commandValidator);
                if (command != null)
                {
                    if (!isRuleApp) Messenger.m(source, "gi "+name+" app "+action+" with /"+name+" command");
                    server.getCommandManager().getDispatcher().register(command);
                    CarpetServer.settingsManager.notifyPlayersCommandsChanged();
                }
                else {
                    if (!isRuleApp) Messenger.m(source, "gi "+name+" app "+action);
                }
            }
            catch (CommandSyntaxException cse)
            {
                removeScriptHost(source, name, false, false);
                Messenger.m(source, "r Failed to build command system for "+name+" thus failed to load the app ", cse.getRawMessage());
                return false;
            }

        }
        else if (!addLegacyCommand(source, name, reload, !isRuleApp, commandValidator)) // this needs to be moved to config reader, only supporting legacy command here
        {
            removeScriptHost(source, name, false, false);
            Messenger.m(source, "r Failed to build command system for "+name+" thus failed to load the app");
            return false;
        }
        if (newHost.isPerUser())
        {
            // that will provide player hosts right at the startup
            newHost.retrieveForExecution(source, null);
        }
        long end = System.nanoTime();
        CarpetSettings.LOG.info("App "+name+" loaded in "+(end-start)/1000000+" ms");
        return true;
    }

    private boolean addLegacyCommand(ServerCommandSource source, String hostName, boolean isReload, boolean notifySource, Function<ServerCommandSource, Boolean> useValidator)
    {
        CarpetScriptHost host = modules.get(hostName);
        String loaded = isReload?"reloaded":"loaded";
        if (host == null)
        {
            return true;
        }
        if (host.getFunction("__command") == null)
        {
            if (notifySource) Messenger.m(source, "gi "+hostName+" app "+loaded+".");
            return true;
        }
        if (holyMoly.contains(hostName))
        {
            Messenger.m(source, "gi "+hostName+" app "+loaded+" with no command.");
            Messenger.m(source, "gi Tried to mask vanilla command.");
            return true;
        }

        Function<ServerCommandSource, Boolean> configValidator;
        try
        {
            configValidator = host.getCommandConfigPermissions();
        }
        catch (CommandSyntaxException e)
        {
            Messenger.m(source, "rb "+e.getMessage());
            return false;
        }

        LiteralArgumentBuilder<ServerCommandSource> command = literal(hostName).
                requires((player) -> modules.containsKey(hostName) && useValidator.apply(player) && configValidator.apply(player)).
                executes( (c) ->
                {
                    CarpetScriptHost targetHost = modules.get(hostName).retrieveOwnForExecution(c.getSource());
                    Value response = targetHost.handleCommandLegacy(c.getSource(),"__command", null, "");
                    if (!response.isNull()) Messenger.m(c.getSource(), "gi "+response.getString());
                    return (int)response.readInteger();
                });

        for (String function : host.globaFunctionNames(host.main, s ->  !s.startsWith("_")).sorted().collect(Collectors.toList()))
        {
            if (host.appConfig.getOrDefault(StringValue.of("legacy_command_type_support"), Value.FALSE).getBoolean())
            {
                try
                {
                    FunctionValue functionValue = host.getFunction(function);
                    command = host.addPathToCommand(
                            command,
                            CommandToken.parseSpec(CommandToken.specFromSignature(functionValue), host),
                            FunctionArgument.fromCommandSpec(host, functionValue)
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
                                requires((player) -> modules.containsKey(hostName) && modules.get(hostName).getFunction(function) != null).
                                executes((c) -> {
                                    CarpetScriptHost targetHost = modules.get(hostName).retrieveOwnForExecution(c.getSource());
                                    Value response = targetHost.handleCommandLegacy(c.getSource(),function, null, "");
                                    if (!response.isNull()) Messenger.m(c.getSource(), "gi " + response.getString());
                                    return (int) response.readInteger();
                                }).
                                then(argument("args...", StringArgumentType.greedyString()).
                                        executes( (c) -> {
                                            CarpetScriptHost targetHost = modules.get(hostName).retrieveOwnForExecution(c.getSource());
                                            Value response = targetHost.handleCommandLegacy(c.getSource(),function, null, StringArgumentType.getString(c, "args..."));
                                            if (!response.isNull()) Messenger.m(c.getSource(), "gi "+response.getString());
                                            return (int)response.readInteger();
                                        })));
            }
        }
        if (notifySource) Messenger.m(source, "gi "+hostName+" app "+loaded+" with /"+hostName+" command");
        server.getCommandManager().getDispatcher().register(command);
        CarpetServer.settingsManager.notifyPlayersCommandsChanged();
        return true;
    }


    public boolean removeScriptHost(ServerCommandSource source, String name, boolean notifySource, boolean isRuleApp)
    {
        name = name.toLowerCase(Locale.ROOT);
        if (!modules.containsKey(name) || (!isRuleApp && !unloadableModules.contains(name)))
        {
            if (notifySource) Messenger.m(source, "r No such app found: ", "wb  " + name);
            return false;
        }
        // stop all events associated with name
        events.removeAllHostEvents(modules.get(name));
        modules.get(name).onClose();
        modules.remove(name);
        if (!isRuleApp) unloadableModules.remove(name);
        CarpetServer.settingsManager.notifyPlayersCommandsChanged();
        if (notifySource) Messenger.m(source, "gi Removed "+name+" app");
        return true;
    }

    public boolean runEventCall(ServerCommandSource sender, String hostname, String optionalTarget, FunctionValue udf, List<Value> argv)
    {
        CarpetScriptHost host = globalHost;
        if (hostname != null)
            host = modules.get(hostname);
        if (host == null) return false;
        // dummy call for player apps that reside on the global copy - do not run them, but report as successes.
        if (host.isPerUser() && optionalTarget==null) return true;
        ServerPlayerEntity target = null;
        if (optionalTarget != null)
        {
            target = sender.getMinecraftServer().getPlayerManager().getPlayer(optionalTarget);
            if (target == null) return false;
        }
        int successes = signal(sender, target, hostname, udf, argv, true );
        return successes >= 0;
    }

    public void runScheduledCall(BlockPos origin, ServerCommandSource source, String hostname, CarpetScriptHost host, FunctionValue udf, List<Value> argv)
    {
        if (hostname != null && !modules.containsKey(hostname)) // well - scheduled call app got unloaded
            return;
        try
        {
            host.callUDF(origin, source, udf, argv);
        }
        catch (NullPointerException | InvalidCallbackException ignored)
        {
        }
    }

    /**
     * returns number of successful calls in the host or -1 if failed exceptionally
     * @param sender
     * @param optionalRecipient
     * @param hostname
     * @param udf
     * @param argv
     * @param reportFails - returns -1 when any call failed
     * @return
     */
    public int signal(ServerCommandSource sender, ServerPlayerEntity optionalRecipient, String hostname, FunctionValue udf, List<Value> argv, boolean reportFails)
    {

        if (hostname == null)
        {
            if (optionalRecipient != null) return 0;
            try
            {
                globalHost.callUDF(BlockPos.ORIGIN, sender, udf, argv);
            }
            catch (NullPointerException | InvalidCallbackException npe)
            {
                return reportFails?-1:0;
            }
            return 1;
        }
        String hostRecipient = optionalRecipient == null?null:optionalRecipient.getEntityName();
        ServerCommandSource source = (optionalRecipient == null)? sender : optionalRecipient.getCommandSource().withLevel(CarpetSettings.runPermissionLevel);
        int successes = 0;
        for (CarpetScriptHost host : modules.get(hostname).retrieveForExecution(sender, hostRecipient))
        {
            // getPlayer will always return nonnull cause retrieve for execution only returns hosts for existing players.
            ServerCommandSource executingSource = host.perUser
                    ? source.getMinecraftServer().getPlayerManager().getPlayer(host.user).getCommandSource()
                    : source.getMinecraftServer().getCommandSource();
            try
            {
                host.callUDF(BlockPos.ORIGIN, source.withLevel(CarpetSettings.runPermissionLevel), udf, argv);
            }
            catch (NullPointerException | InvalidCallbackException npe)
            {
                if (reportFails) return -1;
                continue;
            }
            successes ++;
        }
        return successes;
    }

    public void tick()
    {
        events.tick();
        for (CarpetScriptHost host : modules.values())
        {
            host.tick();
        }
    }

    public void onClose()
    {
        CarpetEventServer.Event.SHUTDOWN.onTick();
        for (ScriptHost host : modules.values())
        {
            host.onClose();
        }
    }

    public void onPlayerJoin(ServerPlayerEntity player)
    {
        modules.values().forEach(h ->
        {
            if (h.isPerUser())
            {
                try
                {
                    h.retrieveOwnForExecution(player.getCommandSource());
                }
                catch (CommandSyntaxException ignored)
                {
                }
            }
        });
    }

    static class TransferData
    {
        boolean perUser;
        Function<ServerCommandSource, Boolean> commandValidator;
        boolean isRuleApp;
        private TransferData(CarpetScriptHost host)
        {
            perUser = host.perUser;
            commandValidator = host.commandValidator;
            isRuleApp = host.isRuleApp;
        }
    }

    public void reload(MinecraftServer server)
    {
        Map<String, TransferData> apps = new HashMap<>();
        modules.forEach((s, h) -> apps.put(s, new TransferData(h)));
        apps.keySet().forEach(s -> removeScriptHost(server.getCommandSource(), s, false, false));
        CarpetEventServer.Event.clearAllBuiltinEvents();
        init();
        apps.forEach((s, data) -> addScriptHost(server.getCommandSource(), s,data.commandValidator, data.perUser,false, data.isRuleApp));
    }
}
