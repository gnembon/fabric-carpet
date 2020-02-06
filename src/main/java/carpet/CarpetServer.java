package carpet;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import carpet.commands.*;
import carpet.helpers.TickSpeed;
import carpet.logging.LoggerRegistry;
import carpet.script.CarpetScriptServer;
import carpet.settings.SettingsManager;
import carpet.utils.HUDController;
import carpet.utils.MobAI;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public class CarpetServer // static for now - easier to handle all around the code, its one anyways
{
    public static final Random rand = new Random();
    public static MinecraftServer minecraft_server;
    private static CommandDispatcher<ServerCommandSource> currentCommandDispatcher;
    public static CarpetScriptServer scriptServer;
    public static SettingsManager settingsManager;
    public static final List<CarpetExtension> extensions = new ArrayList<>();

    // Separate from onServerLoaded, because a server can be loaded multiple times in singleplayer
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
        LoggerRegistry.initLoggers();
        settingsManager = new SettingsManager(CarpetSettings.carpetVersion, "carpet", "Carpet Mod");
        settingsManager.parseSettingsClass(CarpetSettings.class);
        extensions.forEach(CarpetExtension::onGameStarted);
    }

    public static void onServerLoaded(MinecraftServer server)
    {
        CarpetServer.minecraft_server = server;
        settingsManager.attachServer(server);
        extensions.forEach(e -> {
            SettingsManager sm = e.customSettingsManager();
            if (sm != null) sm.attachServer(server);
            e.onServerLoaded(server);
        });
        scriptServer = new CarpetScriptServer();
        scriptServer.loadAllWorldScripts();
        MobAI.resetTrackers();
    }

    public static void tick(MinecraftServer server)
    {
        TickSpeed.tick(server);
        HUDController.update_hud(server);
        scriptServer.tick();

        //in case something happens
        CarpetSettings.impendingFillSkipUpdates = false;
        CarpetSettings.currentTelepotingEntityBox = null;
        CarpetSettings.fixedPosition = null;

        extensions.forEach(e -> e.onTick(server));
    }

    public static void registerCarpetCommands(CommandDispatcher<ServerCommandSource> dispatcher)
    {
        TickCommand.register(dispatcher);
        ProfileCommand.register(dispatcher);
        CounterCommand.register(dispatcher);
        LogCommand.register(dispatcher);
        SpawnCommand.register(dispatcher);
        PlayerCommand.register(dispatcher);
        CameraModeCommand.register(dispatcher);
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
        //TestCommand.register(dispatcher);
    }

    public static void onPlayerLoggedIn(ServerPlayerEntity player)
    {
        LoggerRegistry.playerConnected(player);
        extensions.forEach(e -> e.onPlayerLoggedIn(player));
    }

    public static void onPlayerLoggedOut(ServerPlayerEntity player)
    {
        LoggerRegistry.playerDisconnected(player);
        extensions.forEach(e -> e.onPlayerLoggedOut(player));
    }

    public static void onServerClosed(MinecraftServer server)
    {
        currentCommandDispatcher = null;
        scriptServer.onClose();
        settingsManager.detachServer();
        LoggerRegistry.stopLoggers();
        extensions.forEach(e -> e.onServerClosed(server));
    }
}

