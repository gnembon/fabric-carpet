package carpet.commands;

import carpet.CarpetSettings;
import carpet.fakes.MinecraftServerInterface;
import carpet.helpers.ServerTickRateManager;
import carpet.network.ServerNetworkHandler;
import carpet.utils.CarpetProfiler;
import carpet.utils.CommandHelper;
import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
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
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext)
    {
        LiteralArgumentBuilder<CommandSourceStack> literalargumentbuilder = literal("tick").
                requires((player) -> CommandHelper.canUseCommand(player, CarpetSettings.commandTick)).
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
        ServerTickRateManager trm = ((MinecraftServerInterface)source.getServer()).getTickRateManager();
        trm.setTickRate(tps, true);
        queryTps(source);
        return (int)tps;
    }

    private static int queryTps(CommandSourceStack source)
    {
        ServerTickRateManager trm = ((MinecraftServerInterface)source.getServer()).getTickRateManager();

        Messenger.m(source, "w Current tps is: ",String.format("wb %.1f", trm.tickrate()));
        return (int) trm.tickrate();
    }

    private static int setWarp(CommandSourceStack source, int advance, String tail_command)
    {
        ServerPlayer player = source.getPlayer(); // may be null
        ServerTickRateManager trm = ((MinecraftServerInterface)source.getServer()).getTickRateManager();
        Component message = trm.requestGameToWarpSpeed(player, advance, tail_command, source);
        source.sendSuccess(() -> message, false);
        return 1;
    }

    private static int freezeStatus(CommandSourceStack source)
    {
        ServerTickRateManager trm = ((MinecraftServerInterface)source.getServer()).getTickRateManager();
        if(trm.gameIsPaused())
        {
            Messenger.m(source, "gi Freeze Status: Game is "+(trm.deeplyFrozen()?"deeply ":"")+"frozen");
        }
        else
        {
            Messenger.m(source, "gi Freeze Status: Game runs normally");
        }
        return 1;
    }

    private static int setFreeze(CommandSourceStack source, boolean isDeep, boolean freeze)
    {
        ServerTickRateManager trm = ((MinecraftServerInterface)source.getServer()).getTickRateManager();
        trm.setFrozenState(freeze, isDeep);
        if (trm.gameIsPaused())
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
        ServerTickRateManager trm = ((MinecraftServerInterface)source.getServer()).getTickRateManager();
        return setFreeze(source, isDeep, !trm.gameIsPaused());
    }

    private static int step(CommandSourceStack source, int advance)
    {
        ServerTickRateManager trm = ((MinecraftServerInterface)source.getServer()).getTickRateManager();
        trm.stepGameIfPaused(advance);
        Messenger.m(source, "gi Stepping " + advance + " tick" + (advance != 1 ? "s" : ""));
        return 1;
    }

    private static int toggleSuperHot(CommandSourceStack source)
    {
        ServerTickRateManager trm = ((MinecraftServerInterface)source.getServer()).getTickRateManager();
        trm.setSuperHot(!trm.isSuperHot());
        ServerNetworkHandler.updateSuperHotStateToConnectedPlayers(source.getServer());
        if (trm.isSuperHot())
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
