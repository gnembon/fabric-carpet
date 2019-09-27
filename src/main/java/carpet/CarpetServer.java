package carpet;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import carpet.commands.CameraModeCommand;
import carpet.commands.CounterCommand;
import carpet.commands.DistanceCommand;
import carpet.commands.DrawCommand;
import carpet.commands.InfoCommand;
import carpet.commands.LogCommand;
import carpet.commands.MobAICommand;
import carpet.commands.PerimeterInfoCommand;
import carpet.commands.PlayerCommand;
import carpet.commands.ScriptCommand;
import carpet.commands.SpawnCommand;
import carpet.commands.TickCommand;
import carpet.helpers.TickSpeed;
import carpet.logging.LoggerRegistry;
import carpet.script.CarpetScriptServer;
import carpet.settings.CarpetSettings;
import carpet.settings.SettingsManager;
import carpet.utils.HUDController;
import carpet.utils.MobAI;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public class CarpetServer // static for now - easier to handle all around the code, its one anyways
{
    public static final Random rand = new Random((int)((2>>16)*Math.random()));
    public static MinecraftServer minecraft_server;
    public static CarpetScriptServer scriptServer;
    public static SettingsManager settingsManager;
    public static List<CarpetExtension> extensions = new ArrayList<>();

    // Separate from onServerLoaded, because a server can be loaded multiple times in singleplayer
    public static void manageExtension(CarpetExtension extension)
    {
        extensions.add(extension);
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
        extensions.forEach(e -> e.registerCommands(dispatcher));
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
        LoggerRegistry.stopLoggers();
        extensions.forEach(e -> e.onServerClosed(server));
    }
}

