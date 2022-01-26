package carpet;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
import carpet.settings.SettingsManager;
import carpet.logging.HUDController;
import carpet.utils.FabricAPIHooks;
import carpet.utils.MobAI;
import carpet.utils.SpawnReporter;
import com.mojang.brigadier.CommandDispatcher;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.PerfCommand;
import net.minecraft.server.level.ServerPlayer;

public class CarpetServer // static for now - easier to handle all around the code, its one anyways
{
    public static final ClientModInitializer CLIENT_INITIALIZER = CarpetServer::onGameStarted;
    public static final DedicatedServerModInitializer SERVER_INITIALIZER = CarpetServer::onGameStarted;

    public static final Random rand = new Random();
    public static MinecraftServer minecraft_server;
    private static CommandDispatcher<CommandSourceStack> currentCommandDispatcher;
    public static CarpetScriptServer scriptServer;
    public static SettingsManager settingsManager;
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
        // for extensions that come late to the party, after server is created / loaded
        // we will handle them now.
        // that would handle all extensions, even these that add themselves really late to the party
        if (currentCommandDispatcher != null)
        {
            extension.registerCommands(currentCommandDispatcher);
        }
    }

    public static void onGameStarted()
    {
        settingsManager = new SettingsManager(CarpetSettings.carpetVersion, "carpet", "Carpet Mod");
        settingsManager.parseSettingsClass(CarpetSettings.class);
        extensions.forEach(CarpetExtension::onGameStarted);
        FabricAPIHooks.initialize();
        CarpetScriptServer.parseFunctionClasses();
    }

    public static void onServerLoaded(MinecraftServer server)
    {
        CarpetServer.minecraft_server = server;
        // shoudl not be needed - that bit needs refactoring, but not now.
        SpawnReporter.reset_spawn_stats(server, true);

        settingsManager.attachServer(server);
        extensions.forEach(e -> {
            SettingsManager sm = e.customSettingsManager();
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

    @Deprecated
    public static void registerCarpetCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
    }

    public static void registerCarpetCommands(CommandDispatcher<CommandSourceStack> dispatcher, Commands.CommandSelection environment)
    {
        settingsManager.registerCommand(dispatcher);
        extensions.forEach(e -> {
            SettingsManager sm = e.customSettingsManager();
            if (sm != null) sm.registerCommand(dispatcher);
        });
        TickCommand.register(dispatcher);
        ProfileCommand.register(dispatcher);
        CounterCommand.register(dispatcher);
        LogCommand.register(dispatcher);
        SpawnCommand.register(dispatcher);
        PlayerCommand.register(dispatcher);
        //CameraModeCommand.register(dispatcher);
        InfoCommand.register(dispatcher);
        DistanceCommand.register(dispatcher);
        PerimeterInfoCommand.register(dispatcher);
        DrawCommand.register(dispatcher);
        ScriptCommand.register(dispatcher);
        MobAICommand.register(dispatcher);
        // registering command of extensions that has registered before either server is created
        // for all other, they will have them registered when they add themselves
        extensions.forEach(e -> e.registerCommands(dispatcher));
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
}

