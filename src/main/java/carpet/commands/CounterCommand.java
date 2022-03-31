package carpet.commands;

import carpet.CarpetSettings;
import carpet.helpers.HopperCounter;
import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.BaseComponent;
import net.minecraft.world.item.DyeColor;

/**
 * Class for the /counter command which allows to use hoppers pointing into wool
 */

public class CounterCommand
{
    /**
     * The method used to register the command and make it available for the players to use.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        LiteralArgumentBuilder<CommandSourceStack> literalargumentbuilder = Commands.literal("counter").executes((context)
         -> listAllCounters(context.getSource(), false)).requires((player) ->
                CarpetSettings.hopperCounters);

        literalargumentbuilder.
                then((Commands.literal("reset").executes( (p_198489_1_)->
                        resetCounter(p_198489_1_.getSource(), null))));
        for (DyeColor enumDyeColor: DyeColor.values())
        {
            String color = enumDyeColor.toString();
            literalargumentbuilder.
                    then((Commands.literal(color).executes( (p_198489_1_)-> displayCounter(p_198489_1_.getSource(), color, false))));
            literalargumentbuilder.then(Commands.literal(color).
                    then(Commands.literal("reset").executes((context) ->
                            resetCounter(context.getSource(), color))));
            literalargumentbuilder.then(Commands.literal(color).
                    then(Commands.literal("realtime").executes((context) ->
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

    private static int displayCounter(CommandSourceStack source, String color, boolean realtime)
    {
        HopperCounter counter = HopperCounter.getCounter(color);
        if (counter == null) throw new CommandRuntimeException(Messenger.tr("carpet.command.counter.unknown_wool_color", color));

        Messenger.send(source, counter.format(source.getServer(), realtime, false));
        return 1;
    }

    /**
     * A method to reset the counter's timer to 0 and empty its items. If the {@code color} parameter is {@code null},
     * it will reset all counters.
     * @param color The counter whose contents we want to reset
     */
    private static int resetCounter(CommandSourceStack source, String color)
    {
        if (color == null)
        {
            HopperCounter.resetAll(source.getServer(), false);
            Messenger.m(source, Messenger.tr("carpet.command.counter.restarted_all"));
        }
        else
        {
            HopperCounter counter = HopperCounter.getCounter(color);
            if (counter == null) throw new CommandRuntimeException(Messenger.tr("carpet.command.counter.unknown_wool_color", color));
            counter.reset(source.getServer());
            Messenger.m(source, Messenger.tr("carpet.command.counter.restarted_single", counter.getPrettyName()));
        }
        return 1;
    }

    /**
     * A method to prettily display all the counters to the player
     * @param realtime Whether or not to display it as in-game time or IRL time, which accounts for less than 20TPS which
     *                would make it slower than IRL
     */
    private static int listAllCounters(CommandSourceStack source, boolean realtime)
    {
        Messenger.send(source, HopperCounter.formatAll(source.getServer(), realtime));
        return 1;
    }

}
