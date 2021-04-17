package carpet.commands;

import carpet.CarpetSettings;
import carpet.helpers.HopperCounter;
import carpet.helpers.TickSpeed;
import carpet.settings.SettingsManager;
import carpet.utils.Messenger;
import carpet.utils.SpawnReporter;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.stream.Collectors;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.command.CommandSource.suggestMatching;


public class SpawnCommand
{
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher)
    {
        LiteralArgumentBuilder<ServerCommandSource> literalargumentbuilder = literal("spawn").
                requires((player) -> SettingsManager.canUseCommand(player, CarpetSettings.commandSpawn));

        literalargumentbuilder.
                then(literal("list").
                        then(argument("pos", BlockPosArgumentType.blockPos()).
                                executes( (c) -> listSpawns(c.getSource(), BlockPosArgumentType.getBlockPos(c, "pos"))))).
                then(literal("tracking").
                        executes( (c) -> printTrackingReport(c.getSource())).
                        then(literal("start").
                                executes( (c) -> startTracking(c.getSource(), null, null)).
                                then(argument("from", BlockPosArgumentType.blockPos()).
                                        then(argument("to", BlockPosArgumentType.blockPos()).
                                                executes( (c) -> startTracking(
                                                        c.getSource(),
                                                        BlockPosArgumentType.getBlockPos(c, "from"),
                                                        BlockPosArgumentType.getBlockPos(c, "to")))))).
                        then(literal("stop").
                                executes( (c) -> stopTracking(c.getSource()))).
                        then(argument("type", word()).
                                suggests( (c, b) -> suggestMatching(Arrays.stream(SpawnGroup.values()).map(SpawnGroup::getName),b)).
                                executes( (c) -> recentSpawnsForType(c.getSource(), getString(c, "type"))))).
                then(literal("test").
                        executes( (c)-> runTest(c.getSource(), 72000, null)).
                        then(argument("ticks", integer(10,720000)).
                                executes( (c)-> runTest(
                                        c.getSource(),
                                        getInteger(c, "ticks"),
                                        null)).
                                then(argument("counter", word()).
                                        suggests( (c, b) -> suggestMatching(Arrays.stream(DyeColor.values()).map(DyeColor::toString),b)).
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
                                suggests( (c, b) -> suggestMatching(Arrays.stream(SpawnGroup.values()).map(SpawnGroup::getName),b)).
                                then(argument("rounds", integer(0)).
                                        suggests( (c, b) -> suggestMatching(new String[]{"1"},b)).
                                        executes( (c) -> setSpawnRates(
                                                c.getSource(),
                                                getString(c, "type"),
                                                getInteger(c, "rounds")))))).
                then(literal("mobcaps").
                        executes( (c) -> generalMobcaps(c.getSource())).
                        then(literal("set").
                                then(argument("cap (hostile)", integer(1,1400)).
                                        executes( (c) -> setMobcaps(c.getSource(), getInteger(c, "cap (hostile)"))))).
                        then(argument("dimension", DimensionArgumentType.dimension()).
                                executes( (c)-> mobcapsForDimension(c.getSource(), DimensionArgumentType.getDimensionArgument(c, "dimension"))))).
                then(literal("entities").
                        executes( (c) -> generalMobcaps(c.getSource()) ).
                        then(argument("type", string()).
                                suggests( (c, b)->suggestMatching(Arrays.stream(SpawnGroup.values()).map(SpawnGroup::getName), b)).
                                executes( (c) -> listEntitiesOfType(c.getSource(), getString(c, "type"), false)).
                                then(literal("all").executes( (c) -> listEntitiesOfType(c.getSource(), getString(c, "type"), true)))));

        dispatcher.register(literalargumentbuilder);
    }

    private static SpawnGroup getCategory(String string) throws CommandSyntaxException
    {
        if (!Arrays.stream(SpawnGroup.values()).map(SpawnGroup::getName).collect(Collectors.toSet()).contains(string))
        {
            throw new SimpleCommandExceptionType(Messenger.c("r Wrong mob type: "+string+" should be "+ Arrays.stream(SpawnGroup.values()).map(SpawnGroup::getName).collect(Collectors.joining(", ")))).create();
        }
        return SpawnGroup.valueOf(string.toUpperCase());
    }


    private static int listSpawns(ServerCommandSource source, BlockPos pos)
    {
        Messenger.send(source, SpawnReporter.report(pos, source.getWorld()));
        return 1;
    }

    private static int printTrackingReport(ServerCommandSource source)
    {
        Messenger.send(source, SpawnReporter.tracking_report(source.getWorld()));
        return 1;
    }

    private static int startTracking(ServerCommandSource source, BlockPos a, BlockPos b)
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
        SpawnReporter.reset_spawn_stats(source.getMinecraftServer(), false);
        SpawnReporter.track_spawns = (long) source.getMinecraftServer().getTicks();
        SpawnReporter.lower_spawning_limit = lsl;
        SpawnReporter.upper_spawning_limit = usl;
        Messenger.m(source, "gi Spawning tracking started.");
        return 1;
    }

    private static int stopTracking(ServerCommandSource source)
    {
        Messenger.send(source, SpawnReporter.tracking_report(source.getWorld()));
        SpawnReporter.reset_spawn_stats(source.getMinecraftServer(),false);
        SpawnReporter.track_spawns = 0L;
        SpawnReporter.lower_spawning_limit = null;
        SpawnReporter.upper_spawning_limit = null;
        Messenger.m(source, "gi Spawning tracking stopped.");
        return 1;
    }

    private static int recentSpawnsForType(ServerCommandSource source, String mob_type) throws CommandSyntaxException
    {
        SpawnGroup cat = getCategory(mob_type);
        Messenger.send(source, SpawnReporter.recent_spawns(source.getWorld(), cat));
        return 1;
    }

    private static int runTest(ServerCommandSource source, int ticks, String counter)
    {
        //stop tracking
        SpawnReporter.reset_spawn_stats(source.getMinecraftServer(),false);
        //start tracking
        SpawnReporter.track_spawns = (long) source.getMinecraftServer().getTicks();
        //counter reset
        if (counter == null)
        {
            HopperCounter.resetAll(source.getMinecraftServer(), false);
        }
        else
        {
            HopperCounter hCounter = HopperCounter.getCounter(counter);
            if (hCounter != null)
                    hCounter.reset(source.getMinecraftServer());
        }


        // tick warp 0
        TickSpeed.tickrate_advance(null, 0, null, null);
        // tick warp given player
        ServerCommandSource csource = null;
        ServerPlayerEntity player = null;
        try
        {
            player = source.getPlayer();
            csource = source;
        }
        catch (CommandSyntaxException ignored)
        {
        }
        TickSpeed.tickrate_advance(player, ticks, null, csource);
        Messenger.m(source, String.format("gi Started spawn test for %d ticks", ticks));
        return 1;
    }

    private static int toggleMocking(ServerCommandSource source, boolean domock)
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

    private static int generalMobcaps(ServerCommandSource source)
    {
        Messenger.send(source, SpawnReporter.printMobcapsForDimension(source.getWorld(), true));
        return 1;
    }

    private static int resetSpawnRates(ServerCommandSource source)
    {
        for (SpawnGroup s: SpawnReporter.spawn_tries.keySet())
        {
            SpawnReporter.spawn_tries.put(s,1);
        }
        Messenger.m(source, "gi Spawn rates brought to 1 round per tick for all groups.");

        return 1;
    }

    private static int setSpawnRates(ServerCommandSource source, String mobtype, int rounds) throws CommandSyntaxException
    {
        SpawnGroup cat = getCategory(mobtype);
        SpawnReporter.spawn_tries.put(cat, rounds);
        Messenger.m(source, "gi "+mobtype+" mobs will now spawn "+rounds+" times per tick");
        return 1;
    }

    private static int setMobcaps(ServerCommandSource source, int hostile_cap)
    {
        double desired_ratio = (double)hostile_cap/ SpawnGroup.MONSTER.getCapacity();
        SpawnReporter.mobcap_exponent = 4.0*Math.log(desired_ratio)/Math.log(2.0);
        Messenger.m(source, String.format("gi Mobcaps for hostile mobs changed to %d, other groups will follow", hostile_cap));
        return 1;
    }

    private static int mobcapsForDimension(ServerCommandSource source, ServerWorld world)
    {
        Messenger.send(source, SpawnReporter.printMobcapsForDimension(world, true));
        return 1;
    }

    private static int listEntitiesOfType(ServerCommandSource source, String mobtype, boolean all) throws CommandSyntaxException
    {
        SpawnGroup cat = getCategory(mobtype);
        Messenger.send(source, SpawnReporter.printEntitiesByType(cat, source.getWorld(), all));
        return 1;
    }
}
