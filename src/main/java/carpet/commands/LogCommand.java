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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.entity.player.Player;
import java.util.Arrays;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static net.minecraft.commands.SharedSuggestionProvider.suggest;

public class LogCommand
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        LiteralArgumentBuilder<CommandSourceStack> literalargumentbuilder = Commands.literal("log").
                requires((player) -> SettingsManager.canUseCommand(player, CarpetSettings.commandLog)).
                executes((context) -> listLogs(context.getSource())).
                then(Commands.literal("clear").
                        executes( (c) -> unsubFromAll(c.getSource(), c.getSource().getTextName())).
                        then(Commands.argument("player", StringArgumentType.word()).
                                suggests( (c, b)-> suggest(c.getSource().getOnlinePlayerNames(),b)).
                                executes( (c) -> unsubFromAll(c.getSource(), getString(c, "player")))));

        literalargumentbuilder.then(Commands.argument("log name",StringArgumentType.word()).
                suggests( (c, b)-> suggest(LoggerRegistry.getLoggerNames(),b)).
                executes( (c)-> toggleSubscription(c.getSource(), c.getSource().getTextName(), getString(c, "log name"))).
                then(Commands.literal("clear").
                        executes( (c) -> unsubFromLogger(
                                c.getSource(),
                                c.getSource().getTextName(),
                                getString(c, "log name")))).
                then(Commands.argument("option", StringArgumentType.string()).
                        suggests( (c, b) -> suggest(
                                (LoggerRegistry.getLogger(getString(c, "log name"))==null
                                        ?new String[]{}
                                        :LoggerRegistry.getLogger(getString(c, "log name")).getOptions()),
                                b)).
                        executes( (c) -> subscribePlayer(
                                c.getSource(),
                                c.getSource().getTextName(),
                                getString(c, "log name"),
                                getString(c, "option"))).
                        then(Commands.argument("player", StringArgumentType.word()).
                                suggests( (c, b) -> suggest(c.getSource().getOnlinePlayerNames(),b)).
                                executes( (c) -> subscribePlayer(
                                        c.getSource(),
                                        getString(c, "player"),
                                        getString(c, "log name"),
                                        getString(c, "option"))))));

        dispatcher.register(literalargumentbuilder);
    }
    private static int listLogs(CommandSourceStack source)
    {
        Player player;
        try
        {
            player = source.getPlayerOrException();
        }
        catch (CommandSyntaxException e)
        {
            Messenger.m(source, Messenger.tr("carpet.command.log.player_only"));
            return 0;
        }
        Map<String,String> subs = LoggerRegistry.getPlayerSubscriptions(source.getTextName());
        if (subs == null)
        {
            subs = new HashMap<>();
        }
        List<String> all_logs = new ArrayList<>(LoggerRegistry.getLoggerNames());
        Collections.sort(all_logs);
        Messenger.m(player, "w _____________________");
        Messenger.m(player, Messenger.tr("carpet.command.log.listing_header"));
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
                    comp.add("l ");
                    comp.add(Messenger.tr("carpet.command.log.subscribed_state"));
                }
                else
                {
                    comp.add(color);
                    comp.add(Messenger.c(" [", Messenger.tr("carpet.command.log.subscribe_button"), " ]"));
                    comp.add("^w");
                    comp.add(Messenger.tr("carpet.command.log.subscribe_hint", lname));
                    comp.add("!/log " + lname);
                }
                comp.add(Messenger.s(" "));
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
                        comp.add("^w");
                        comp.add(Messenger.tr("carpet.command.log.subscribe_hint_with_option", lname, option));
                        comp.add("!/log " + lname + " " + option);
                    }

                }
            }
            if (subs.containsKey(lname))
            {
                comp.add("nb [X]");
                comp.add("^w");
                comp.add(Messenger.tr("carpet.command.log.unsubscribe_hint", lname));
                comp.add("!/log "+lname);
            }
            Messenger.m(player,comp.toArray(new Object[0]));
        }
        return 1;
    }

    private static boolean checkPlayer(CommandSourceStack source, String player_name)
    {
        Player player = source.getServer().getPlayerList().getPlayerByName(player_name);
        if (player == null)
        {
            Messenger.m(source, "r", Messenger.tr("carpet.command.log.no_player_specified", player_name));
            return false;
        }
        return true;
    }
    private static boolean checkLogger(CommandSourceStack source, String logName)
    {
        if (LoggerRegistry.getLogger(logName) == null)
        {
            Messenger.m(source, "r", Messenger.tr("carpet.command.log.unknown_logger", Messenger.c("rb " + logName)));
            return false;
        }
        return true;
    }

    private static int unsubFromAll(CommandSourceStack source, String player_name)
    {
        if (!checkPlayer(source, player_name)) return 0;
        for (String logname : LoggerRegistry.getLoggerNames())
        {
            LoggerRegistry.unsubscribePlayer(player_name, logname);
        }
        Messenger.m(source, "gi", Messenger.tr("carpet.command.log.unsubscribed_all"));
        return 1;
    }
    private static int unsubFromLogger(CommandSourceStack source, String player_name, String logname)
    {
        if (!checkPlayer(source, player_name)) return 0;
        if (!checkLogger(source, logname)) return 0;
        LoggerRegistry.unsubscribePlayer(player_name, logname);
        Messenger.m(source, "gi", Messenger.tr("carpet.command.log.unsubscribed_single", logname));
        return 1;
    }

    private static int toggleSubscription(CommandSourceStack source, String player_name, String logName)
    {
        if (!checkPlayer(source, player_name)) return 0;
        if (!checkLogger(source, logName)) return 0;
        boolean subscribed = LoggerRegistry.togglePlayerSubscription(player_name, logName);
        if (subscribed)
        {
            Messenger.m(source, "gi", Messenger.tr("carpet.command.log.toggle_subscribe", player_name, logName));
        }
        else
        {
            Messenger.m(source, "gi", Messenger.tr("carpet.command.log.toggle_unsubscribe", player_name, logName));
        }
        return 1;
    }
    private static int subscribePlayer(CommandSourceStack source, String player_name, String logname, String option)
    {
        if (!checkPlayer(source, player_name)) return 0;
        if (!checkLogger(source, logname)) return 0;
        if (!LoggerRegistry.getLogger(logname).isOptionValid(option))
        {
            Messenger.m(source, "r", Messenger.tr("carpet.command.log.invalid_option", Messenger.c("rb "+option)));
            return 0;
        }
        LoggerRegistry.subscribePlayer(player_name, logname, option);
        if (option != null)
        {
            Messenger.m(source, "gi", Messenger.tr("carpet.command.log.subscribed_to_with_option", logname, option));
        }
        else
        {
            Messenger.m(source, "gi", Messenger.tr("carpet.command.log.subscribed_to", logname));
        }
        return 1;
    }
}
