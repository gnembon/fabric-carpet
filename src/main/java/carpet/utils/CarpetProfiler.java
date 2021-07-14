package carpet.utils;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class CarpetProfiler
{
    private static final Map<String, Long> SECTION_STATS = new HashMap<>();
    private static final Object2LongMap<Pair<World,Object>> ENTITY_TIMES = new Object2LongOpenHashMap<>();
    private static final Object2LongMap<Pair<World,Object>> ENTITY_COUNT = new Object2LongOpenHashMap<>();


    private static ServerCommandSource currentRequester = null;
    public static int tick_health_requested = 0;
    private static int tick_health_elapsed = 0;
    private static TYPE test_type = TYPE.NONE; //1 for ticks, 2 for entities
    private static long current_tick_start = 0;
    private static final String[] GENERAL_SECTIONS = {"Network", "Autosave", "Async Tasks", "Datapacks", "Carpet"};
    private static final String[] SCARPET_SECTIONS = {
            "Scarpet run", "Scarpet events", "Scarpet schedule",
            "Scarpet command", "Scarpet load", "Scarpet app data", "Scarpet client"
    };
    private static final String[] SECTIONS = {
            "Spawning and Random Ticks", "Ticket Manager","Unloading",
            "Blocks", "Entities", "Block Entities",
            "Entities (Client)", "Block Entities (Client)",
            "Village", "Environment"};

    public enum TYPE
    {
        NONE,
        GENERAL,
        ENTITY,
        TILEENTITY
    }

    public static class ProfilerToken
    {
        public final TYPE type;
        public final Object section;
        public final long start;
        public final World world;

        public ProfilerToken(TYPE type, Object section, World world)
        {
            this.type = type;
            this.section = section;
            this.start = System.nanoTime();
            this.world = world;
        }
    }

    public static void prepare_tick_report(ServerCommandSource source, int ticks)
    {
        //maybe add so it only spams the sending player, but honestly - all may want to see it
        SECTION_STATS.clear();
        ENTITY_COUNT.clear();
        ENTITY_TIMES.clear();
        test_type = TYPE.GENERAL;
        SECTION_STATS.put("tick", 0L);
        for (String section : GENERAL_SECTIONS)
        {
            SECTION_STATS.put(section, 0L);
        }
        for (String section : SCARPET_SECTIONS)
        {
            SECTION_STATS.put(section, 0L);
        }
        for (RegistryKey<World> level : source.getServer().getWorldRegistryKeys())
        {
            for (String section : SECTIONS)
            {
                SECTION_STATS.put(level.getValue() + "." + section, 0L);
            }
        }

        tick_health_elapsed = ticks;
        tick_health_requested = ticks;
        current_tick_start = 0L;
        currentRequester = source;
    }

    public static void prepare_entity_report(ServerCommandSource source, int ticks)
    {
        //maybe add so it only spams the sending player, but honestly - all may want to see it
        SECTION_STATS.clear();
        SECTION_STATS.put("tick", 0L);
        ENTITY_COUNT.clear();
        ENTITY_TIMES.clear();
        test_type = TYPE.ENTITY;
        tick_health_elapsed = ticks;
        tick_health_requested = ticks;
        current_tick_start = 0L;
        currentRequester = source;
    }

    public static ProfilerToken start_section(World world, String name, TYPE type)
    {
        if (tick_health_requested == 0L || test_type != TYPE.GENERAL || current_tick_start == 0)
            return null;
        return new ProfilerToken(type, name, world);
    }

    public static ProfilerToken start_entity_section(World world, Entity e, TYPE type)
    {
        if (tick_health_requested == 0L || test_type != TYPE.ENTITY || current_tick_start == 0)
            return null;
        return new ProfilerToken(type, e.getType(), world);
    }

    public static ProfilerToken start_block_entity_section(World world, BlockEntity be, TYPE type)
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
            World world = tok.world;
            String current_section = (world == null) ?
                    (String) tok.section :
                    String.format("%s.%s%s", world.getRegistryKey().getValue(), tok.section, world.isClient ? " (Client)" : "");
            SECTION_STATS.put(current_section, SECTION_STATS.getOrDefault(current_section, 0L) + end_time - tok.start);
        }
    }

    public static void end_current_entity_section(ProfilerToken tok)
    {
        if (tick_health_requested == 0L || test_type != TYPE.ENTITY || current_tick_start == 0 || tok == null)
            return;
        long end_time = System.nanoTime();
        Pair<World,Object> section = Pair.of(tok.world, tok.section);
        ENTITY_TIMES.put(section, ENTITY_TIMES.getOrDefault(section, 0L) + end_time - tok.start);
        ENTITY_COUNT.put(section, ENTITY_COUNT.getOrDefault(section, 0L) + 1);
    }

    public static void start_tick_profiling()
    {
        current_tick_start = System.nanoTime();
    }

    public static void end_tick_profiling(MinecraftServer server)
    {
        if (current_tick_start == 0L)
            return;
        SECTION_STATS.put("tick", SECTION_STATS.get("tick") + System.nanoTime() - current_tick_start);
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
        SECTION_STATS.put("tick", 0L);
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
        long total_tick_time = SECTION_STATS.get("tick");
        double divider = 1.0D / tick_health_requested / 1000000;
        Messenger.m(currentRequester, "w ");
        Messenger.m(currentRequester, "wb Average tick time: ", String.format("yb %.3fms", divider * total_tick_time));
        long accumulated = 0L;

        for (String section : GENERAL_SECTIONS)
        {
            double amount = divider * SECTION_STATS.get(section);
            if (amount > 0.01)
            {
                accumulated += SECTION_STATS.get(section);
                Messenger.m(currentRequester, "w "+section+": ", String.format("y %.3fms", amount));
            }
        }
        for (String section : SCARPET_SECTIONS)
        {
            double amount = divider * SECTION_STATS.get(section);
            if (amount > 0.01)
            {
                Messenger.m(currentRequester, "gi "+section+": ", String.format("di %.3fms", amount));
            }
        }

        for (RegistryKey<World> dim : server.getWorldRegistryKeys())
        {
            Identifier dimensionId = dim.getValue();
            boolean hasSomethin = false;
            for (String section : SECTIONS)
            {

                double amount = divider * SECTION_STATS.getOrDefault(dimensionId + "." + section, 0L);
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
            for (String section : SECTIONS)
            {
                double amount = divider * SECTION_STATS.getOrDefault(dimensionId + "." + section, 0L);
                if (amount > 0.01)
                {
                    boolean cli = section.endsWith("(Client)");
                    if (!cli)
                        accumulated += SECTION_STATS.get(dimensionId + "." + section);
                    Messenger.m(currentRequester, String.format("%s - %s: ", cli?"gi":"w", section), String.format("%s %.3fms", cli?"di":"y", amount));
                }
            }
        }

        long rest = total_tick_time - accumulated;

        Messenger.m(currentRequester, String.format("gi The Rest, whatever that might be: %.3fms", divider * rest));
    }

    private static String sectionName(Pair<World,Object> section)
    {
        Identifier id;
        if (section.getValue() instanceof EntityType)
        {
            id = Registry.ENTITY_TYPE.getId((EntityType<?>) section.getValue());
        }
        else
        {
            id = Registry.BLOCK_ENTITY_TYPE.getId((BlockEntityType<?>) section.getValue());
        }
        String name = "minecraft".equals(id.getNamespace())?id.getPath():id.toString();
        if (section.getKey().isClient)
        {
            name += " (client)";
        }
        Identifier dimkey = section.getKey().getRegistryKey().getValue();
        String dim = "minecraft".equals(dimkey.getNamespace())?dimkey.getPath():dimkey.toString();
        return name+" in "+dim;
    }

    public static void finalize_tick_report_for_entities(MinecraftServer server)
    {
        if (currentRequester == null)
            return;
        long total_tick_time = SECTION_STATS.get("tick");
        double divider = 1.0D / tick_health_requested / 1000000;
        double divider_1 = 1.0D / (tick_health_requested - 1) / 1000000;
        Messenger.m(currentRequester, "w ");
        Messenger.m(currentRequester, "wb Average tick time: ", String.format("yb %.3fms", divider * total_tick_time));
        SECTION_STATS.remove("tick");
        Messenger.m(currentRequester, "wb Top 10 counts:");
        int total = 0;
        for (Object2LongMap.Entry<Pair<World, Object>> sectionEntry : ENTITY_COUNT.object2LongEntrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).collect(Collectors.toList()))
        {
            if (++total > 10) break;
            Pair<World,Object> section = sectionEntry.getKey();
            boolean cli = section.getKey().isClient;
            Messenger.m(currentRequester, String.format(
                    "%s - %s: ", cli?"gi":"w",
                    sectionName(section)),
                    String.format("%s %.1f", cli?"di":"y",
                    1.0D * sectionEntry.getLongValue() / (tick_health_requested - (cli? 1 : 0))
            ));
        }
        Messenger.m(currentRequester, "wb Top 10 CPU hogs:");
        total = 0;
        for (Object2LongMap.Entry<Pair<World,Object>> sectionEntry : ENTITY_TIMES.object2LongEntrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).collect(Collectors.toList()))
        {
            if (++total > 10) break;
            Pair<World,Object> section = sectionEntry.getKey();
            boolean cli = section.getKey().isClient;
            Messenger.m(currentRequester, String.format(
                    "%s - %s: ", cli?"gi":"w",
                    sectionName(section)),
                    String.format("%s %.2fms", cli?"di":"y",
                    (cli ? divider : divider_1) * sectionEntry.getLongValue()
            ));
        }
    }
}