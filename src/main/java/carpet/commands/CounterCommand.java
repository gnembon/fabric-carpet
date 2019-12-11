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

public class CounterCommand
{
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

    private static int displayCounter(ServerCommandSource source, String color, boolean realtime)
    {
        HopperCounter counter = HopperCounter.getCounter(color);
        if (counter == null) throw new CommandException(Messenger.s("Unknown wool color: "+color));

        for (BaseText message: counter.format(source.getMinecraftServer(), realtime, false))
        {
            source.sendFeedback(message, false);
        }
        return 1;
    }

    private static int resetCounter(ServerCommandSource source, String color)
    {
        if (color == null)
        {
            HopperCounter.resetAll(source.getMinecraftServer());
            Messenger.m(source, "w Restarted all counters");
        }
        else
        {
            HopperCounter counter = HopperCounter.getCounter(color);
            if (counter == null) throw new CommandException(Messenger.s("Unknown wool color"));
            counter.reset(source.getMinecraftServer());
            Messenger.m(source, "w Restarted "+color+" counter");
        }
        return 1;
    }

    private static int listAllCounters(ServerCommandSource source, boolean realtime)
    {
        for (BaseText message: HopperCounter.formatAll(source.getMinecraftServer(), realtime))
        {
            source.sendFeedback(message, false);
        }
        return 1;
    }

}
