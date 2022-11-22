package carpet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import carpet.commands.CounterCommand;
import carpet.commands.DistanceCommand;
import carpet.commands.DrawCommand;
import carpet.commands.InfoCommand;
import carpet.commands.LogCommand;
import carpet.commands.MobAICommand;
import carpet.commands.PerimeterInfoCommand;
import carpet.commands.PlayerCommand;
import carpet.commands.ProfileCommand;
import carpet.commands.ScriptCommand;
import carpet.commands.SpawnCommand;
import carpet.commands.TestCommand;
import carpet.commands.TickCommand;
import carpet.network.ServerNetworkHandler;
import carpet.helpers.HopperCounter;
import carpet.helpers.TickSpeed;
import carpet.logging.LoggerRegistry;
import carpet.script.CarpetScriptServer;
import carpet.api.settings.SettingsManager;
import carpet.logging.HUDController;
import carpet.utils.FabricAPIHooks;
import carpet.utils.MobAI;
import carpet.utils.SpawnReporter;
import com.mojang.brigadier.CommandDispatcher;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.PerfCommand;
import net.minecraft.server.level.ServerPlayer;

public class CarpetServer // static for now - easier to handle all around the code, its one anyways
{
    public static MinecraftServer minecraft_server;
    private static CommandDispatcher<CommandSourceStack> currentCommandDispatcher;
    public static CarpetScriptServer scriptServer;
    public static carpet.settings.SettingsManager settingsManager; // to change type to api type, can't change right now because of binary and source compat
    public static final List<CarpetExtension> extensions = new ArrayList<>();

    // Separate from onServerLoaded, because a server can be loaded multiple times in singleplayer
    /**
     * Registers a {@link CarpetExtension} to be managed by Carpet.<br>
     * Should be called before Carpet's startup, like in Fabric Loader's
     * {@link net.fabricmc.api.ModInitializer} entrypoint
     * @param extension The instance of a {@link CarpetExtension} to be registered
     */
    public static void manageExtension(CarpetExtension extension)
    {
        extensions.add(extension);
        // Stop the stupid practice of extensions mixing into Carpet just to register themselves
        if (StackWalker.getInstance().walk(stream -> stream.anyMatch(el -> 
            el.getClassName() == CarpetServer.class.getName() && !el.getMethodName().equals("manageExtension")
        ))) {
            CarpetSettings.LOG.warn("""
                    Extension '%s' is registering itself using a mixin into Carpet instead of a regular ModInitializer!
                    This is stupid and will crash the game in future versions!""".formatted(extension.getClass().getSimpleName()));
        }

        // for extensions that come late to the party, we used to handle them, but we've been giving them warnings about
        // it for a while. Cause a crash
        if (currentCommandDispatcher != null)
        {
            throw new IllegalStateException("Extension %s tried to register too late!".formatted(extension.getClass().getSimpleName()));
        }
    }

    // Gets called by Fabric Loader from a ServerModInitializer and a ClientModInitializer, in both to allow extensions 
    // to register before this call in a ModInitializer (declared in fabric.mod.json)
    public static void onGameStarted()
    {
        settingsManager = new carpet.settings.SettingsManager(CarpetSettings.carpetVersion, "carpet", "Carpet Mod");
        settingsManager.parseSettingsClass(CarpetSettings.class);
        extensions.forEach(CarpetExtension::onGameStarted);
        //FabricAPIHooks.initialize();
        CarpetScriptServer.parseFunctionClasses();
    }

    public static void onServerLoaded(MinecraftServer server)
    {
        CarpetServer.minecraft_server = server;
        // shoudl not be needed - that bit needs refactoring, but not now.
        SpawnReporter.reset_spawn_stats(server, true);

        settingsManager.attachServer(server);
        extensions.forEach(e -> {
        	SettingsManager sm = e.extensionSettingsManager();
            if (sm != null) sm.attachServer(server);
            e.onServerLoaded(server);
        });
        scriptServer = new CarpetScriptServer(server);
        MobAI.resetTrackers();
        LoggerRegistry.initLoggers();
        //TickSpeed.reset();
    }

    public static void onServerLoadedWorlds(MinecraftServer minecraftServer)
    {
        HopperCounter.resetAll(minecraftServer, true);
        extensions.forEach(e -> e.onServerLoadedWorlds(minecraftServer));
        scriptServer.initializeForWorld();
    }

    public static void tick(MinecraftServer server)
    {
        TickSpeed.tick();
        HUDController.update_hud(server, null);
        if (scriptServer != null) scriptServer.tick();

        //in case something happens
        CarpetSettings.impendingFillSkipUpdates.set(false);

        extensions.forEach(e -> e.onTick(server));
    }

    public static void registerCarpetCommands(CommandDispatcher<CommandSourceStack> dispatcher, Commands.CommandSelection environment, CommandBuildContext commandBuildContext)
    {
        if (settingsManager == null) // bootstrap dev initialization check
        {
            return;
        }
        settingsManager.registerCommand(dispatcher, commandBuildContext);
        extensions.forEach(e -> {
        	SettingsManager sm = e.extensionSettingsManager();
            if (sm != null) sm.registerCommand(dispatcher, commandBuildContext);
        });
        TickCommand.register(dispatcher, commandBuildContext);
        ProfileCommand.register(dispatcher, commandBuildContext);
        CounterCommand.register(dispatcher, commandBuildContext);
        LogCommand.register(dispatcher, commandBuildContext);
        SpawnCommand.register(dispatcher, commandBuildContext);
        PlayerCommand.register(dispatcher, commandBuildContext);
        InfoCommand.register(dispatcher, commandBuildContext);
        DistanceCommand.register(dispatcher, commandBuildContext);
        PerimeterInfoCommand.register(dispatcher, commandBuildContext);
        DrawCommand.register(dispatcher, commandBuildContext);
        ScriptCommand.register(dispatcher, commandBuildContext);
        MobAICommand.register(dispatcher, commandBuildContext);
        // registering command of extensions that has registered before either server is created
        // for all other, they will have them registered when they add themselves
        extensions.forEach(e -> {
            e.registerCommands(dispatcher, commandBuildContext);
        });
        currentCommandDispatcher = dispatcher;

        if (environment != Commands.CommandSelection.DEDICATED)
            PerfCommand.register(dispatcher);
        
        if (FabricLoader.getInstance().isDevelopmentEnvironment())
            TestCommand.register(dispatcher);
        // todo 1.16 - re-registerer apps if that's a reload operation.
    }

    public static void onPlayerLoggedIn(ServerPlayer player)
    {
        ServerNetworkHandler.onPlayerJoin(player);
        LoggerRegistry.playerConnected(player);
        scriptServer.onPlayerJoin(player);
        extensions.forEach(e -> e.onPlayerLoggedIn(player));

    }

    public static void onPlayerLoggedOut(ServerPlayer player)
    {
        ServerNetworkHandler.onPlayerLoggedOut(player);
        LoggerRegistry.playerDisconnected(player);
        extensions.forEach(e -> e.onPlayerLoggedOut(player));
    }

    public static void clientPreClosing()
    {
        if (scriptServer != null) scriptServer.onClose();
        scriptServer = null;
    }

    public static void onServerClosed(MinecraftServer server)
    {
        // this for whatever reason gets called multiple times even when joining on SP
        // so we allow to pass multiple times gating it only on existing server ref
        if (minecraft_server != null)
        {
            if (scriptServer != null) scriptServer.onClose();
            scriptServer = null;
            ServerNetworkHandler.close();
            currentCommandDispatcher = null;

            LoggerRegistry.stopLoggers();
            HUDController.resetScarpetHUDs();
            extensions.forEach(e -> e.onServerClosed(server));
            minecraft_server = null;
        }

        // this for whatever reason gets called multiple times even when joining;
        TickSpeed.reset();
    }
    public static void onServerDoneClosing(MinecraftServer server)
    {
        settingsManager.detachServer();
        extensions.forEach(e -> {
        	SettingsManager manager = e.extensionSettingsManager();
            if (manager != null) manager.detachServer();
        });
    }

    public static void registerExtensionLoggers()
    {
        extensions.forEach(CarpetExtension::registerLoggers);
    }

    public static void onReload(MinecraftServer server)
    {
        scriptServer.reload(server);
        extensions.forEach(e -> e.onReload(server));
    }
    
    private static final Set<CarpetExtension> warnedOutdatedManagerProviders = new HashSet<>();
    static void warnOutdatedManager(CarpetExtension ext)
    {
        if (warnedOutdatedManagerProviders.add(ext))
            CarpetSettings.LOG.warn("""
                    %s is providing a SettingsManager from an outdated method in CarpetExtension!
                    This behaviour will not work in later Carpet versions and the manager won't be registered!""".formatted(ext.getClass().getName()));
    }
}

