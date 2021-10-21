package carpet.commands;

import carpet.CarpetSettings;
import carpet.logging.Logger;
import carpet.logging.LoggerRegistry;
import carpet.settings.SettingsManager;
import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static net.minecraft.command.CommandSource.suggestMatching;

public class LogCommand
{
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher)
    {
        LiteralArgumentBuilder<ServerCommandSource> literalargumentbuilder = CommandManager.literal("log").
                requires((player) -> SettingsManager.canUseCommand(player, CarpetSettings.commandLog)).
                executes((context) -> listLogs(context.getSource())).
                then(CommandManager.literal("clear").
                        executes( (c) -> unsubFromAll(c.getSource(), c.getSource().getName())).
                        then(CommandManager.argument("player", StringArgumentType.word()).
                                suggests( (c, b)-> suggestMatching(c.getSource().getPlayerNames(),b)).
                                executes( (c) -> unsubFromAll(c.getSource(), getString(c, "player")))));

        literalargumentbuilder.then(CommandManager.argument("log name",StringArgumentType.word()).
                suggests( (c, b)-> suggestMatching(LoggerRegistry.getLoggerNames(),b)).
                executes( (c)-> toggleSubscription(c.getSource(), c.getSource().getName(), getString(c, "log name"))).
                then(CommandManager.literal("clear").
                        executes( (c) -> unsubFromLogger(
                                c.getSource(),
                                c.getSource().getName(),
                                getString(c, "log name")))).
                then(CommandManager.argument("option", StringArgumentType.string()).
                        suggests( (c, b) -> suggestMatching(
                                (LoggerRegistry.getLogger(getString(c, "log name"))==null
                                        ?new String[]{}
                                        :LoggerRegistry.getLogger(getString(c, "log name")).getOptions()),
                                b)).
                        executes( (c) -> subscribePlayer(
                                c.getSource(),
                                c.getSource().getName(),
                                getString(c, "log name"),
                                getString(c, "option"))).
                        then(CommandManager.argument("player", StringArgumentType.word()).
                                suggests( (c, b) -> suggestMatching(c.getSource().getPlayerNames(),b)).
                                executes( (c) -> subscribePlayer(
                                        c.getSource(),
                                        getString(c, "player"),
                                        getString(c, "log name"),
                                        getString(c, "option"))))));

        dispatcher.register(literalargumentbuilder);
    }
    private static int listLogs(ServerCommandSource source)
    {
        PlayerEntity player;
        try
        {
            player = source.getPlayer();
        }
        catch (CommandSyntaxException e)
        {
            Messenger.m(source, "For players only");
            return 0;
        }
        Map<String,String> subs = LoggerRegistry.getPlayerSubscriptions(source.getName());
        if (subs == null)
        {
            subs = new HashMap<>();
        }
        List<String> all_logs = new ArrayList<>(LoggerRegistry.getLoggerNames());
        Collections.sort(all_logs);
        Messenger.m(player, "w _____________________");
        Messenger.m(player, "w Available logging options:");
        for (String lname: all_logs)
        {
            List<Object> comp = new ArrayList<>();
            String color = subs.containsKey(lname)?"w":"g";
            comp.add("w  - "+lname+": ");
            Logger logger = LoggerRegistry.getLogger(lname);
            String [] options = logger.getOptions();
            if (options.length == 0)
            {
                if (subs.containsKey(lname))
                {
                    comp.add("l Subscribed ");
                }
                else
                {
                    comp.add(color + " [Subscribe] ");
                    comp.add("^w subscribe to " + lname);
                    comp.add("!/log " + lname);
                }
            }
            else
            {
                for (String option : logger.getOptions())
                {
                    if (subs.containsKey(lname) && subs.get(lname).equalsIgnoreCase(option))
                    {
                        comp.add("l [" + option + "] ");
                    } else
                    {
                        comp.add(color + " [" + option + "] ");
                        comp.add("^w subscribe to " + lname + " " + option);
                        comp.add("!/log " + lname + " " + option);
                    }

                }
            }
            if (subs.containsKey(lname))
            {
                comp.add("nb [X]");
                comp.add("^w Click to unsubscribe");
                comp.add("!/log "+lname);
            }
            Messenger.m(player,comp.toArray(new Object[0]));
        }
        return 1;
    }
    private static int unsubFromAll(ServerCommandSource source, String player_name)
    {
        PlayerEntity player = source.getServer().getPlayerManager().getPlayer(player_name);
        if (player == null)
        {
            Messenger.m(source, "r No player specified");
            return 0;
        }
        for (String logname : LoggerRegistry.getLoggerNames())
        {
            LoggerRegistry.unsubscribePlayer(player_name, logname);
        }
        Messenger.m(source, "gi Unsubscribed from all logs");
        return 1;
    }
    private static int unsubFromLogger(ServerCommandSource source, String player_name, String logname)
    {
        PlayerEntity player = source.getServer().getPlayerManager().getPlayer(player_name);
        if (player == null)
        {
            Messenger.m(source, "r No player specified");
            return 0;
        }
        if (LoggerRegistry.getLogger(logname) == null)
        {
            Messenger.m(source, "r Unknown logger: ","rb "+logname);
            return 0;
        }
        LoggerRegistry.unsubscribePlayer(player_name, logname);
        Messenger.m(source, "gi Unsubscribed from "+logname);
        return 1;
    }

    private static int toggleSubscription(ServerCommandSource source, String player_name, String logName)
    {
        PlayerEntity player = source.getServer().getPlayerManager().getPlayer(player_name);
        if (player == null)
        {
            Messenger.m(source, "r No player specified");
            return 0;
        }
        if (LoggerRegistry.getLogger(logName) == null)
        {
            Messenger.m(source, "r Unknown logger: ","rb "+logName);
            return 0;
        }
        boolean subscribed = LoggerRegistry.togglePlayerSubscription(player_name, logName);
        if (subscribed)
        {
            Messenger.m(source, "gi "+player_name+" subscribed to " + logName + ".");
        }
        else
        {
            Messenger.m(source, "gi "+player_name+" unsubscribed from " + logName + ".");
        }
        return 1;
    }
    private static int subscribePlayer(ServerCommandSource source, String player_name, String logname, String option)
    {
        PlayerEntity player = source.getServer().getPlayerManager().getPlayer(player_name);
        if (player == null)
        {
            Messenger.m(source, "r No player specified");
            return 0;
        }
        if (LoggerRegistry.getLogger(logname) == null)
        {
            Messenger.m(source, "r Unknown logger: ","rb "+logname);
            return 0;
        }
        if (!LoggerRegistry.getLogger(logname).isOptionValid(option))
        {
            Messenger.m(source, "r Invalid option: ", "rb "+option);
            return 0;
        }
        LoggerRegistry.subscribePlayer(player_name, logname, option);
        if (option != null)
        {
            Messenger.m(source, "gi Subscribed to " + logname + "(" + option + ")");
        }
        else
        {
            Messenger.m(source, "gi Subscribed to " + logname);
        }
        return 1;
    }
}
