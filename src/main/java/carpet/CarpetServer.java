package carpet;

import carpet.commands.CameraModeCommand;
//import carpet.commands.CarpetCommand;
//import carpet.commands.CounterCommand;
//import carpet.commands.DistanceCommand;
import carpet.commands.DrawCommand;
//import carpet.commands.ScriptCommand;
//import carpet.commands.InfoCommand;
//import carpet.commands.PerimeterInfoCommand;
//import carpet.commands.PlayerCommand;
//import carpet.commands.SpawnCommand;
import carpet.commands.LogCommand;
import carpet.commands.TestCommand;
//import carpet.commands.TickCommand;
//import carpet.logging.LoggerRegistry;
//import carpet.script.ExpressionInspector;
//import carpet.utils.HUDController;

import java.util.Random;

//import carpet.helpers.TickSpeed;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.MinecraftServer;

public class CarpetServer // static for now - easier to handle all around the code, its one anyways
{
    public static final Random rand = new Random((int)((2>>16)*Math.random()));
    public static MinecraftServer minecraft_server;
    public static void init(MinecraftServer server) //aka constructor of this static singleton class
    {
        CarpetServer.minecraft_server = server;
    }
    public static void onServerLoaded(MinecraftServer server)
    {
        //CarpetSettings.apply_settings_from_conf(server);
        //ExpressionInspector.CarpetExpression_resetExpressionEngine();
    }
    // Separate from onServerLoaded, because a server can be loaded multiple times in singleplayer
    public static void onGameStarted() {
        //LoggerRegistry.initLoggers();
    }

    public static void tick(MinecraftServer server)
    {
        //TickSpeed.tick(server);
        //HUDController.update_hud(server);
    }

    public static void registerCarpetCommands(CommandDispatcher<ServerCommandSource> dispatcher)
    {
        //CarpetCommand.register(dispatcher);
        //TickCommand.register(dispatcher);
        //CounterCommand.register(dispatcher);
        LogCommand.register(dispatcher);
        //SpawnCommand.register(dispatcher);
        //PlayerCommand.register(dispatcher);
        CameraModeCommand.register(dispatcher);
        //InfoCommand.register(dispatcher);
        //DistanceCommand.register(dispatcher);
        //PerimeterInfoCommand.register(dispatcher);
        DrawCommand.register(dispatcher);
        //ScriptCommand.register(dispatcher);

        TestCommand.register(dispatcher);
    }
}

