package carpet.utils;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Comparator;
import java.util.Map;

import static java.util.Map.entry;

public class CarpetProfiler
{
    private static final Object2LongOpenHashMap<String> SECTION_STATS = new Object2LongOpenHashMap<>();
    private static final Object2LongOpenHashMap<Pair<Level,Object>> ENTITY_TIMES = new Object2LongOpenHashMap<>();
    private static final Object2LongOpenHashMap<Pair<Level,Object>> ENTITY_COUNT = new Object2LongOpenHashMap<>();


    private static CommandSourceStack currentRequester = null;
    public static int tick_health_requested = 0;
    private static int tick_health_elapsed = 0;
    private static TYPE test_type = TYPE.NONE; //1 for ticks, 2 for entities
    private static long current_tick_start = 0;
    private static final Map<String, String> GENERAL_SECTIONS = Map.of(
        "Network",     "Packet sending, player logins, disconnects, kicks, anti-cheat check for player movement, etc.",
        "Autosave",    "Autosave",
        "Async Tasks", "Various asynchronous tasks on the server. Mainly chunk generation, chunk saving, etc.",
        "Datapacks",   "Datapack tick function execution. Load function execution if reload was performed.",
        "Carpet",      "Player hud, scripts, and extensions (If they choose to use carpet's onTick)."
    );

    private static final Map<String, String> SCARPET_SECTIONS = Map.of(
        "Scarpet run",      "script run command execution",
        "Scarpet events",   "script events, custom or built-in",
        "Scarpet schedule", "script scheduled calls/events",
        "Scarpet command",  "script custom commands. Calls, executions, suggestions, etc.",
        "Scarpet load",     "script and libraries (if required) loading",
        "Scarpet app data", "script module data (if required) ticking and saving",
        "Scarpet client",   "script shape rendering. (Client side)"
    );

    private static final Map<String, String> SECTIONS = Map.ofEntries(
        entry("Spawning",                "Spawning of various things. Natural mobs, cats, patrols, wandering traders, phantoms, skeleton horses, etc."),
        entry("Random Ticks",            "Random ticks. Both block random ticks and fluid random ticks."),
        entry("Ticket Manager",          "Chunk ticket manager. Assigning tickets, removing tickets, etc."),
        entry("Unloading",               "POI ticking and chunk unloading."),
        entry("Schedule Ticks",          "Scheduled ticks. Repeaters, observers, redstone torch, water, lava, etc."),
        entry("Block Events",            "Scheduled Block events. Pistons, comparators, noteblocks, block entity events (chests opening/closing), etc."),
        entry("Entities",                "All the entities in the server. Ticking, removing, despawning, dragon fight (if active), etc."),
        entry("Block Entities",          "All the block entities in the server. Removal, ticking, etc."),
        entry("Entities (Client)",       "Entity lag client side. Mostly rendering."),
        entry("Block Entities (Client)", "Block entity lag client side. Mostly rendering."),
        entry("Raid",                    "Raid ticking, stopping, etc."),
        entry("Environment",             "Weather, time, waking up players, water freezing, cauldron filling, snow layers, etc.")
    );

    public enum TYPE
    {
        NONE,
        GENERAL,
        ENTITY,
        TILEENTITY
    }

    public static record ProfilerToken(TYPE type, Object section, long start, Level world)
    {
        public ProfilerToken(TYPE type, Object section, Level world)
        {
            this(type, section, System.nanoTime(), world);
        }
    }

    public static void prepare_tick_report(CommandSourceStack source, int ticks)
    {
        //maybe add so it only spams the sending player, but honestly - all may want to see it
        SECTION_STATS.clear(); // everything then defaults to 0
        ENTITY_COUNT.clear();
        ENTITY_TIMES.clear();
        test_type = TYPE.GENERAL;

        tick_health_elapsed = ticks;
        tick_health_requested = ticks;
        current_tick_start = 0L;
        currentRequester = source;
    }

    public static void prepare_entity_report(CommandSourceStack source, int ticks)
    {
        //maybe add so it only spams the sending player, but honestly - all may want to see it
        SECTION_STATS.clear();
        ENTITY_COUNT.clear();
        ENTITY_TIMES.clear();
        test_type = TYPE.ENTITY;
        tick_health_elapsed = ticks;
        tick_health_requested = ticks;
        current_tick_start = 0L;
        currentRequester = source;
    }

    public static ProfilerToken start_section(Level world, String name, TYPE type)
    {
        if (tick_health_requested == 0L || test_type != TYPE.GENERAL || current_tick_start == 0)
            return null;
        return new ProfilerToken(type, name, world);
    }

    public static ProfilerToken start_entity_section(Level world, Entity e, TYPE type)
    {
        if (tick_health_requested == 0L || test_type != TYPE.ENTITY || current_tick_start == 0)
            return null;
        return new ProfilerToken(type, e.getType(), world);
    }

    public static ProfilerToken start_block_entity_section(Level world, BlockEntity be, TYPE type)
    {
        if (tick_health_requested == 0L || test_type != TYPE.ENTITY || current_tick_start == 0)
            return null;
        return new ProfilerToken(type, be.getType(), world);
    }

    public static void end_current_section(ProfilerToken tok)
    {
        if (tick_health_requested == 0L || test_type != TYPE.GENERAL || current_tick_start == 0 || tok == null)
            return;
        long end_time = System.nanoTime();
        if (tok.type == TYPE.GENERAL)
        {
            Level world = tok.world;
            String current_section = (world == null) ?
                    (String) tok.section :
                    String.format("%s.%s%s", world.dimension().location(), tok.section, world.isClientSide ? " (Client)" : "");
            SECTION_STATS.addTo(current_section, end_time - tok.start);
        }
    }

    public static void end_current_entity_section(ProfilerToken tok)
    {
        if (tick_health_requested == 0L || test_type != TYPE.ENTITY || current_tick_start == 0 || tok == null)
            return;
        long end_time = System.nanoTime();
        Pair<Level,Object> section = Pair.of(tok.world, tok.section);
        ENTITY_TIMES.addTo(section, end_time - tok.start);
        ENTITY_COUNT.addTo(section, 1);
    }

    public static void start_tick_profiling()
    {
        current_tick_start = System.nanoTime();
    }

    public static void end_tick_profiling(MinecraftServer server)
    {
        if (current_tick_start == 0L)
            return;
        SECTION_STATS.addTo("tick", System.nanoTime() - current_tick_start);
        tick_health_elapsed--;
        if (tick_health_elapsed <= 0)
        {
            finalize_tick_report(server);
        }
    }

    public static void finalize_tick_report(MinecraftServer server)
    {
        if (test_type == TYPE.GENERAL)
            finalize_tick_report_for_time(server);
        if (test_type == TYPE.ENTITY)
            finalize_tick_report_for_entities(server);
        cleanup_tick_report();
    }

    public static void cleanup_tick_report()
    {
        SECTION_STATS.clear();
        ENTITY_TIMES.clear();
        ENTITY_COUNT.clear();
        test_type = TYPE.NONE;
        tick_health_elapsed = 0;
        tick_health_requested = 0;
        current_tick_start = 0L;
        currentRequester = null;
    }

    public static void finalize_tick_report_for_time(MinecraftServer server)
    {
        //print stats
        if (currentRequester == null)
            return;
        long total_tick_time = SECTION_STATS.getLong("tick");
        double divider = 1.0D / tick_health_requested / 1000000;
        Messenger.m(currentRequester, "w ");
        Messenger.m(currentRequester, "wb Average tick time: ", String.format("yb %.3fms", divider * total_tick_time));
        long accumulated = 0L;

        for (String section : GENERAL_SECTIONS.keySet())
        {
            double amount = divider * SECTION_STATS.getLong(section);
            if (amount > 0.01)
            {
                accumulated += SECTION_STATS.getLong(section);
                Messenger.m(
                        currentRequester,
                        "w " + section + ": ",
                        "^ " + GENERAL_SECTIONS.get(section),
                        "y %.3fms".formatted(amount)
                );
            }
        }
        for (String section : SCARPET_SECTIONS.keySet())
        {
            double amount = divider * SECTION_STATS.getLong(section);
            if (amount > 0.01)
            {
                Messenger.m(
                        currentRequester,
                        "gi "+section+": ",
                        "^ " + SCARPET_SECTIONS.get(section),
                        "di %.3fms".formatted(amount)
                );
            }
        }

        for (ResourceKey<Level> dim : server.levelKeys())
        {
            ResourceLocation dimensionId = dim.location();
            boolean hasSomethin = false;
            for (String section : SECTIONS.keySet())
            {
                double amount = divider * SECTION_STATS.getLong(dimensionId + "." + section);
                if (amount > 0.01)
                {
                    hasSomethin = true;
                    break;
                }
            }
            if (!(hasSomethin))
            {
                continue;
            }
            Messenger.m(currentRequester, "wb "+(dimensionId.getNamespace().equals("minecraft")?dimensionId.getPath():dimensionId.toString()) + ":");
            for (String section : SECTIONS.keySet())
            {
                double amount = divider * SECTION_STATS.getLong(dimensionId + "." + section);
                if (amount > 0.01)
                {
                    boolean cli = section.endsWith("(Client)");
                    if (!cli)
                        accumulated += SECTION_STATS.getLong(dimensionId + "." + section);
                    Messenger.m(
                            currentRequester,
                            "%s - %s: ".formatted(cli ? "gi" : "w", section),
                            "^ " + SECTIONS.get(section),
                            "%s %.3fms".formatted(cli ? "di" : "y", amount)
                    );
                }
            }
        }

        long rest = total_tick_time - accumulated;

        Messenger.m(currentRequester, String.format("gi The Rest, whatever that might be: %.3fms", divider * rest));
    }

    private static String sectionName(Pair<Level,Object> section)
    {
        ResourceLocation id;
        if (section.getValue() instanceof EntityType)
        {
            id = BuiltInRegistries.ENTITY_TYPE.getKey((EntityType<?>) section.getValue());
        }
        else
        {
            id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey((BlockEntityType<?>) section.getValue());
        }
        String name = "minecraft".equals(id.getNamespace())?id.getPath():id.toString();
        if (section.getKey().isClientSide)
        {
            name += " (client)";
        }
        ResourceLocation dimkey = section.getKey().dimension().location();
        String dim = "minecraft".equals(dimkey.getNamespace())?dimkey.getPath():dimkey.toString();
        return name+" in "+dim;
    }

    public static void finalize_tick_report_for_entities(MinecraftServer server)
    {
        if (currentRequester == null)
            return;
        long total_tick_time = SECTION_STATS.getLong("tick");
        double divider = 1.0D / tick_health_requested / 1000000;
        double divider_1 = 1.0D / (tick_health_requested - 1) / 1000000;
        Messenger.m(currentRequester, "w ");
        Messenger.m(currentRequester, "wb Average tick time: ", String.format("yb %.3fms", divider * total_tick_time));
        SECTION_STATS.removeLong("tick");
        Messenger.m(currentRequester, "wb Top 10 counts:");
        int total = 0;
        for (Object2LongMap.Entry<Pair<Level, Object>> sectionEntry : sortedByValue(ENTITY_COUNT))
        {
            if (++total > 10) break;
            Pair<Level,Object> section = sectionEntry.getKey();
            boolean cli = section.getKey().isClientSide;
            Messenger.m(currentRequester, String.format(
                    "%s - %s: ", cli?"gi":"w",
                    sectionName(section)),
                    String.format("%s %.1f", cli?"di":"y",
                    1.0D * sectionEntry.getLongValue() / (tick_health_requested - (cli? 1 : 0))
            ));
        }
        Messenger.m(currentRequester, "wb Top 10 CPU hogs:");
        total = 0;
        for (Object2LongMap.Entry<Pair<Level, Object>> sectionEntry : sortedByValue(ENTITY_TIMES))
        {
            if (++total > 10) break;
            Pair<Level,Object> section = sectionEntry.getKey();
            boolean cli = section.getKey().isClientSide;
            Messenger.m(currentRequester, String.format(
                    "%s - %s: ", cli?"gi":"w",
                    sectionName(section)),
                    String.format("%s %.2fms", cli?"di":"y",
                    (cli ? divider : divider_1) * sectionEntry.getLongValue()
            ));
        }
    }

    private static <T> Iterable<Object2LongMap.Entry<T>> sortedByValue(Object2LongMap<T> mapToSort) {
        return () -> mapToSort
                .object2LongEntrySet()
                .stream()
                .sorted(Comparator.<Object2LongMap.Entry<T>>comparingLong(Object2LongMap.Entry::getLongValue).reversed())
                .iterator();
    }
}
