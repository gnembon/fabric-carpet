package carpet.commands;

import carpet.CarpetSettings;
import carpet.helpers.TickSpeed;
import carpet.network.ServerNetworkHandler;
import carpet.settings.SettingsManager;
import carpet.utils.CarpetProfiler;
import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.BaseComponent;
import net.minecraft.server.level.ServerPlayer;

import static com.mojang.brigadier.arguments.FloatArgumentType.floatArg;
import static com.mojang.brigadier.arguments.FloatArgumentType.getFloat;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.SharedSuggestionProvider.suggest;

public class TickCommand
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        LiteralArgumentBuilder<CommandSourceStack> literalargumentbuilder = literal("tick").
                requires((player) -> SettingsManager.canUseCommand(player, CarpetSettings.commandTick)).
                then(literal("rate").
                        executes((c) -> queryTps(c.getSource())).
                        then(argument("rate", floatArg(0.1F, 500.0F)).
                                suggests( (c, b) -> suggest(new String[]{"20.0"},b)).
                                executes((c) -> setTps(c.getSource(), getFloat(c, "rate"))))).
                then(literal("warp").
                        executes( (c)-> setWarp(c.getSource(), 0, null)).
                        then(argument("ticks", integer(0)).
                                suggests( (c, b) -> suggest(new String[]{"3600","72000"},b)).
                                executes((c) -> setWarp(c.getSource(), getInteger(c,"ticks"), null)).
                                then(argument("tail command", greedyString()).
                                        executes( (c) -> setWarp(
                                                c.getSource(),
                                                getInteger(c,"ticks"),
                                                getString(c, "tail command")))))).
                then(literal("freeze").executes( (c)-> toggleFreeze(c.getSource(), false)).
                        then(literal("status").executes( (c) -> freezeStatus(c.getSource()))).
                        then(literal("deep").executes( (c) -> toggleFreeze(c.getSource(), true))).
                        then(literal("on").executes( (c) -> setFreeze(c.getSource(), false, true)).
                            then(literal("deep").executes( (c)-> setFreeze(c.getSource(), true, true)))).
                        then(literal("off").executes( (c) -> setFreeze(c.getSource(), false, false)))).
                then(literal("step").
                        executes((c) -> step(c.getSource(), 1)).
                        then(argument("ticks", integer(1,72000)).
                                suggests( (c, b) -> suggest(new String[]{"20"},b)).
                                executes((c) -> step(c.getSource(), getInteger(c,"ticks"))))).
                then(literal("superHot").executes( (c)-> toggleSuperHot(c.getSource()))).
                then(literal("health").
                        executes( (c) -> healthReport(c.getSource(), 100)).
                        then(argument("ticks", integer(20,24000)).
                                executes( (c) -> healthReport(c.getSource(), getInteger(c, "ticks"))))).
                then(literal("entities").
                        executes((c) -> healthEntities(c.getSource(), 100)).
                        then(argument("ticks", integer(20,24000)).
                                executes((c) -> healthEntities(c.getSource(), getInteger(c, "ticks")))));


        dispatcher.register(literalargumentbuilder);
    }


    private static int setTps(CommandSourceStack source, float tps)
    {
        TickSpeed.tickrate(tps, true);
        queryTps(source);
        return (int)tps;
    }

    private static int queryTps(CommandSourceStack source)
    {
        Messenger.m(source, "w Current tps is: ",String.format("wb %.1f", TickSpeed.tickrate));
        return (int)TickSpeed.tickrate;
    }

    private static int setWarp(CommandSourceStack source, int advance, String tail_command)
    {
        ServerPlayer player = null;
        try
        {
            player = source.getPlayerOrException();
        }
        catch (CommandSyntaxException ignored)
        {
        }
        BaseComponent message = TickSpeed.tickrate_advance(player, advance, tail_command, source);
        source.sendSuccess(message, false);
        return 1;
    }

    private static int freezeStatus(CommandSourceStack source)
    {
        if(TickSpeed.isPaused())
        {
            Messenger.m(source, "gi Freeze Status: Game is "+(TickSpeed.deeplyFrozen()?"deeply ":"")+"frozen");
        }
        else
        {
            Messenger.m(source, "gi Freeze Status: Game runs normally");
        }
        return 1;
    }

    private static int setFreeze(CommandSourceStack source, boolean isDeep, boolean freeze)
    {
        TickSpeed.setFrozenState(freeze, isDeep);
        if (TickSpeed.isPaused())
        {
            Messenger.m(source, "gi Game is "+(isDeep?"deeply ":"")+"frozen");
        }
        else
        {
            Messenger.m(source, "gi Game runs normally");
        }
        return 1;
    }

    private static int toggleFreeze(CommandSourceStack source, boolean isDeep)
    {
        return setFreeze(source, isDeep, !TickSpeed.isPaused());
    }

    private static int step(CommandSourceStack source, int advance)
    {
        TickSpeed.add_ticks_to_run_in_pause(advance);
        Messenger.m(source, "gi Stepping " + advance + " tick" + (advance != 1 ? "s" : ""));
        return 1;
    }

    private static int toggleSuperHot(CommandSourceStack source)
    {
        TickSpeed.is_superHot = !TickSpeed.is_superHot;
        ServerNetworkHandler.updateSuperHotStateToConnectedPlayers();
        if (TickSpeed.is_superHot)
        {
            Messenger.m(source, "gi Superhot enabled");
        }
        else
        {
            Messenger.m(source, "gi Superhot disabled");
        }
        return 1;
    }

    public static int healthReport(CommandSourceStack source, int ticks)
    {
        CarpetProfiler.prepare_tick_report(source, ticks);
        return 1;
    }

    public static int healthEntities(CommandSourceStack source, int ticks)
    {
        CarpetProfiler.prepare_entity_report(source, ticks);
        return 1;
    }

}
