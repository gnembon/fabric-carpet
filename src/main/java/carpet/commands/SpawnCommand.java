package carpet.commands;

import carpet.CarpetSettings;
import carpet.fakes.SpawnGroupInterface;
import carpet.helpers.HopperCounter;
import carpet.helpers.TickSpeed;
import carpet.utils.CommandHelper;
import carpet.utils.Messenger;
import carpet.utils.SpawnReporter;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.DyeColor;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.SharedSuggestionProvider.suggest;

public class SpawnCommand
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext)
    {
        LiteralArgumentBuilder<CommandSourceStack> literalargumentbuilder = literal("spawn").
                requires((player) -> CommandHelper.canUseCommand(player, CarpetSettings.commandSpawn));

        literalargumentbuilder.
                then(literal("list").
                        then(argument("pos", BlockPosArgument.blockPos()).
                                executes( (c) -> listSpawns(c.getSource(), BlockPosArgument.getSpawnablePos(c, "pos"))))).
                then(literal("tracking").
                        executes( (c) -> printTrackingReport(c.getSource())).
                        then(literal("start").
                                executes( (c) -> startTracking(c.getSource(), null, null)).
                                then(argument("from", BlockPosArgument.blockPos()).
                                        then(argument("to", BlockPosArgument.blockPos()).
                                                executes( (c) -> startTracking(
                                                        c.getSource(),
                                                        BlockPosArgument.getSpawnablePos(c, "from"),
                                                        BlockPosArgument.getSpawnablePos(c, "to")))))).
                        then(literal("stop").
                                executes( (c) -> stopTracking(c.getSource()))).
                        then(argument("type", word()).
                                suggests( (c, b) -> suggest(Arrays.stream(MobCategory.values()).map(MobCategory::getName),b)).
                                executes( (c) -> recentSpawnsForType(c.getSource(), getString(c, "type"))))).
                then(literal("test").
                        executes( (c)-> runTest(c.getSource(), 72000, null)).
                        then(argument("ticks", integer(10)).
                                executes( (c)-> runTest(
                                        c.getSource(),
                                        getInteger(c, "ticks"),
                                        null)).
                                then(argument("counter", word()).
                                        suggests( (c, b) -> suggest(Arrays.stream(DyeColor.values()).map(DyeColor::toString),b)).
                                        executes((c)-> runTest(
                                                c.getSource(),
                                                getInteger(c, "ticks"),
                                                getString(c, "counter")))))).
                then(literal("mocking").
                        then(argument("to do or not to do?", BoolArgumentType.bool()).
                            executes( (c) -> toggleMocking(c.getSource(), BoolArgumentType.getBool(c, "to do or not to do?"))))).
                then(literal("rates").
                        executes( (c) -> generalMobcaps(c.getSource())).
                        then(literal("reset").
                                executes( (c) -> resetSpawnRates(c.getSource()))).
                        then(argument("type", word()).
                                suggests( (c, b) -> suggest(Arrays.stream(MobCategory.values()).map(MobCategory::getName),b)).
                                then(argument("rounds", integer(0)).
                                        suggests( (c, b) -> suggest(new String[]{"1"},b)).
                                        executes( (c) -> setSpawnRates(
                                                c.getSource(),
                                                getString(c, "type"),
                                                getInteger(c, "rounds")))))).
                then(literal("mobcaps").
                        executes( (c) -> generalMobcaps(c.getSource())).
                        then(literal("set").
                                then(argument("cap (hostile)", integer(1,1400)).
                                        executes( (c) -> setMobcaps(c.getSource(), getInteger(c, "cap (hostile)"))))).
                        then(argument("dimension", DimensionArgument.dimension()).
                                executes( (c)-> mobcapsForDimension(c.getSource(), DimensionArgument.getDimension(c, "dimension"))))).
                then(literal("entities").
                        executes( (c) -> generalMobcaps(c.getSource()) ).
                        then(argument("type", string()).
                                suggests( (c, b)->suggest(Arrays.stream(MobCategory.values()).map(MobCategory::getName), b)).
                                executes( (c) -> listEntitiesOfType(c.getSource(), getString(c, "type"), false)).
                                then(literal("all").executes( (c) -> listEntitiesOfType(c.getSource(), getString(c, "type"), true)))));

        dispatcher.register(literalargumentbuilder);
    }

    private static final Map<String, MobCategory> MOB_CATEGORY_MAP = Arrays.stream(MobCategory.values()).collect(Collectors.toMap(MobCategory::getName, Function.identity()));

    private static MobCategory getCategory(String string) throws CommandSyntaxException
    {
        if (!Arrays.stream(MobCategory.values()).map(MobCategory::getName).collect(Collectors.toSet()).contains(string))
        {
            throw new SimpleCommandExceptionType(Messenger.c("r Wrong mob type: "+string+" should be "+ Arrays.stream(MobCategory.values()).map(MobCategory::getName).collect(Collectors.joining(", ")))).create();
        }
        return MOB_CATEGORY_MAP.get(string.toLowerCase(Locale.ROOT));
    }


    private static int listSpawns(CommandSourceStack source, BlockPos pos)
    {
        Messenger.send(source, SpawnReporter.report(pos, source.getLevel()));
        return 1;
    }

    private static int printTrackingReport(CommandSourceStack source)
    {
        Messenger.send(source, SpawnReporter.tracking_report(source.getLevel()));
        return 1;
    }

    private static int startTracking(CommandSourceStack source, BlockPos a, BlockPos b)
    {
        if (SpawnReporter.track_spawns != 0L)
        {
            Messenger.m(source, "r You are already tracking spawning.");
            return 0;
        }
        BlockPos lsl = null;
        BlockPos usl = null;
        if (a != null && b != null)
        {
            lsl = new BlockPos(
                    Math.min(a.getX(), b.getX()),
                    Math.min(a.getY(), b.getY()),
                    Math.min(a.getZ(), b.getZ()) );
            usl = new BlockPos(
                    Math.max(a.getX(), b.getX()),
                    Math.max(a.getY(), b.getY()),
                    Math.max(a.getZ(), b.getZ()) );
        }
        SpawnReporter.reset_spawn_stats(source.getServer(), false);
        SpawnReporter.track_spawns = (long) source.getServer().getTickCount();
        SpawnReporter.lower_spawning_limit = lsl;
        SpawnReporter.upper_spawning_limit = usl;
        Messenger.m(source, "gi Spawning tracking started.");
        return 1;
    }

    private static int stopTracking(CommandSourceStack source)
    {
        Messenger.send(source, SpawnReporter.tracking_report(source.getLevel()));
        SpawnReporter.reset_spawn_stats(source.getServer(),false);
        SpawnReporter.track_spawns = 0L;
        SpawnReporter.lower_spawning_limit = null;
        SpawnReporter.upper_spawning_limit = null;
        Messenger.m(source, "gi Spawning tracking stopped.");
        return 1;
    }

    private static int recentSpawnsForType(CommandSourceStack source, String mob_type) throws CommandSyntaxException
    {
        MobCategory cat = getCategory(mob_type);
        Messenger.send(source, SpawnReporter.recent_spawns(source.getLevel(), cat));
        return 1;
    }

    private static int runTest(CommandSourceStack source, int ticks, String counter)
    {
        //stop tracking
        SpawnReporter.reset_spawn_stats(source.getServer(),false);
        //start tracking
        SpawnReporter.track_spawns = (long) source.getServer().getTickCount();
        //counter reset
        if (counter == null)
        {
            HopperCounter.resetAll(source.getServer(), false);
        }
        else
        {
            HopperCounter hCounter = HopperCounter.getCounter(counter);
            if (hCounter != null)
                    hCounter.reset(source.getServer());
        }


        // tick warp 0
        TickSpeed.tickrate_advance(null, 0, null, null);
        // tick warp given player
        CommandSourceStack csource = null;
        ServerPlayer player = null;
        try
        {
            player = source.getPlayerOrException();
            csource = source;
        }
        catch (CommandSyntaxException ignored)
        {
        }
        TickSpeed.tickrate_advance(player, ticks, null, csource);
        Messenger.m(source, String.format("gi Started spawn test for %d ticks", ticks));
        return 1;
    }

    private static int toggleMocking(CommandSourceStack source, boolean domock)
    {
        if (domock)
        {
            SpawnReporter.initialize_mocking();
            Messenger.m(source, "gi Mob spawns will now be mocked.");
        }
        else
        {
            SpawnReporter.stop_mocking();
            Messenger.m(source, "gi Normal mob spawning.");
        }
        return 1;
    }

    private static int generalMobcaps(CommandSourceStack source)
    {
        Messenger.send(source, SpawnReporter.printMobcapsForDimension(source.getLevel(), true));
        return 1;
    }

    private static int resetSpawnRates(CommandSourceStack source)
    {
        for (MobCategory s: SpawnReporter.spawn_tries.keySet())
        {
            SpawnReporter.spawn_tries.put(s,1);
        }
        Messenger.m(source, "gi Spawn rates brought to 1 round per tick for all groups.");

        return 1;
    }

    private static int setSpawnRates(CommandSourceStack source, String mobtype, int rounds) throws CommandSyntaxException
    {
        MobCategory cat = getCategory(mobtype);
        SpawnReporter.spawn_tries.put(cat, rounds);
        Messenger.m(source, "gi "+mobtype+" mobs will now spawn "+rounds+" times per tick");
        return 1;
    }

    private static int setMobcaps(CommandSourceStack source, int hostile_cap)
    {
        double desired_ratio = (double)hostile_cap/ ((SpawnGroupInterface)(Object)MobCategory.MONSTER).getInitialSpawnCap();
        SpawnReporter.mobcap_exponent = 4.0*Math.log(desired_ratio)/Math.log(2.0);
        Messenger.m(source, String.format("gi Mobcaps for hostile mobs changed to %d, other groups will follow", hostile_cap));
        return 1;
    }

    private static int mobcapsForDimension(CommandSourceStack source, ServerLevel world)
    {
        Messenger.send(source, SpawnReporter.printMobcapsForDimension(world, true));
        return 1;
    }

    private static int listEntitiesOfType(CommandSourceStack source, String mobtype, boolean all) throws CommandSyntaxException
    {
        MobCategory cat = getCategory(mobtype);
        Messenger.send(source, SpawnReporter.printEntitiesByType(cat, source.getLevel(), all));
        return 1;
    }
}
