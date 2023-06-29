package carpet.commands;

import carpet.CarpetSettings;
import carpet.helpers.HopperCounter;
import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;

import static net.minecraft.commands.Commands.literal;

/**
 * Class for the /counter command which allows to use hoppers pointing into wool
 */
public class CounterCommand
{
    /**
     * The method used to register the command and make it available for the players to use.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext)
    {
        LiteralArgumentBuilder<CommandSourceStack> commandBuilder = literal("counter")
                .requires(c -> CarpetSettings.hopperCounters)
                .executes(c -> listAllCounters(c.getSource(), false))
                .then(literal("reset")
                        .executes(c -> resetCounters(c.getSource())));

        for (DyeColor dyeColor : DyeColor.values())
        {
            commandBuilder.then(
                    literal(dyeColor.toString())
                            .executes(c -> displayCounter(c.getSource(), dyeColor, false))
                            .then(literal("reset")
                                    .executes(c -> resetCounter(c.getSource(), dyeColor)))
                            .then(literal("realtime")
                                    .executes(c -> displayCounter(c.getSource(), dyeColor, true)))
                    );
        }
        dispatcher.register(commandBuilder);
    }

    /**
     * A method to prettily display the contents of a counter to the player
     * @param color The counter colour whose contents we are querying.
     * @param realtime Whether or not to display it as in-game time or IRL time, which accounts for less than 20TPS which
     *                would make it slower than IRL
     */

    private static int displayCounter(CommandSourceStack source, DyeColor color, boolean realtime)
    {
        HopperCounter counter = HopperCounter.getCounter(color);

        for (Component message: counter.format(source.getServer(), realtime, false))
        {
            source.sendSuccess(() -> message, false);
        }
        return 1;
    }

    private static int resetCounters(CommandSourceStack source)
    {
        HopperCounter.resetAll(source.getServer(), false);
        Messenger.m(source, "w Restarted all counters");
        return 1;
    }

    /**
     * A method to reset the counter's timer to 0 and empty its items
     * 
     * @param color The counter whose contents we want to reset
     */
    private static int resetCounter(CommandSourceStack source, DyeColor color)
    {
        HopperCounter.getCounter(color).reset(source.getServer());
        Messenger.m(source, "w Restarted " + color + " counter");
        return 1;
    }

    /**
     * A method to prettily display all the counters to the player
     * @param realtime Whether or not to display it as in-game time or IRL time, which accounts for less than 20TPS which
     *                would make it slower than IRL
     */
    private static int listAllCounters(CommandSourceStack source, boolean realtime)
    {
        for (Component message: HopperCounter.formatAll(source.getServer(), realtime))
        {
            source.sendSuccess(() -> message, false);
        }
        return 1;
    }
}
