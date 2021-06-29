package carpet.commands;

import carpet.CarpetSettings;
import carpet.helpers.HopperCounter;
import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.util.DyeColor;
import net.minecraft.text.BaseText;

/**
 * Class for the /counter command which allows to use hoppers pointing into wool
 */

public class CounterCommand
{
    /**
     * The method used to register the command and make it available for the players to use.
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher)
    {
        LiteralArgumentBuilder<ServerCommandSource> literalargumentbuilder = CommandManager.literal("counter").executes((context)
         -> listAllCounters(context.getSource(), false)).requires((player) ->
                CarpetSettings.hopperCounters);

        literalargumentbuilder.
                then((CommandManager.literal("reset").executes( (p_198489_1_)->
                        resetCounter(p_198489_1_.getSource(), null))));
        for (DyeColor enumDyeColor: DyeColor.values())
        {
            String color = enumDyeColor.toString();
            literalargumentbuilder.
                    then((CommandManager.literal(color).executes( (p_198489_1_)-> displayCounter(p_198489_1_.getSource(), color, false))));
            literalargumentbuilder.then(CommandManager.literal(color).
                    then(CommandManager.literal("reset").executes((context) ->
                            resetCounter(context.getSource(), color))));
            literalargumentbuilder.then(CommandManager.literal(color).
                    then(CommandManager.literal("realtime").executes((context) ->
                            displayCounter(context.getSource(), color, true))));
        }
        dispatcher.register(literalargumentbuilder);
    }

    /**
     * A method to prettily display the contents of a counter to the player
     * @param color The counter colour whose contents we are querying.
     * @param realtime Whether or not to display it as in-game time or IRL time, which accounts for less than 20TPS which
     *                would make it slower than IRL
     */

    private static int displayCounter(ServerCommandSource source, String color, boolean realtime)
    {
        HopperCounter counter = HopperCounter.getCounter(color);
        if (counter == null) throw new CommandException(Messenger.s("Unknown wool color: "+color));

        for (BaseText message: counter.format(source.getServer(), realtime, false))
        {
            source.sendFeedback(message, false);
        }
        return 1;
    }

    /**
     * A method to reset the counter's timer to 0 and empty its items. If the {@code color} parameter is {@code null},
     * it will reset all counters.
     * @param color The counter whose contents we want to reset
     */
    private static int resetCounter(ServerCommandSource source, String color)
    {
        if (color == null)
        {
            HopperCounter.resetAll(source.getServer(), false);
            Messenger.m(source, "w Restarted all counters");
        }
        else
        {
            HopperCounter counter = HopperCounter.getCounter(color);
            if (counter == null) throw new CommandException(Messenger.s("Unknown wool color"));
            counter.reset(source.getServer());
            Messenger.m(source, "w Restarted "+color+" counter");
        }
        return 1;
    }

    /**
     * A method to prettily display all the counters to the player
     * @param realtime Whether or not to display it as in-game time or IRL time, which accounts for less than 20TPS which
     *                would make it slower than IRL
     */
    private static int listAllCounters(ServerCommandSource source, boolean realtime)
    {
        for (BaseText message: HopperCounter.formatAll(source.getServer(), realtime))
        {
            source.sendFeedback(message, false);
        }
        return 1;
    }

}
