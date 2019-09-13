package carpet.utils;

import carpet.settings.CarpetSettings;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class CarpetProfiler
{
    private static final HashMap<String, Long> time_repo = new HashMap<>();
    private static ServerCommandSource currentRequester = null;
    public static int tick_health_requested = 0;
    private static int tick_health_elapsed = 0;
    private static TYPE test_type = TYPE.NONE; //1 for ticks, 2 for entities;
    private static long current_tick_start = 0;
    private static String[] GENERAL_SECTIONS = {"Network", "Autosave", "Async Tasks"};
    private static String[] DIMENSIONS = {"overworld", "the_end", "the_nether"};
    private static String[] SECTIONS = {
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
        public TYPE type;
        public Object section;
        public long start;
        public World world;

        public ProfilerToken(TYPE type, Object section, World world, long start)
        {
            this.type = type;
            this.section = section;
            this.start = start;
            this.world = world;
        }
    }

    private static String getWorldString(World world)
    {
        return world.getDimension().getType().toString().replaceFirst("minecraft:", "") + (world.isClient ? "(Client)" : "");
    }

    private static String getSectionString(World world, String section)
    {
        return String.format("%s.%s%s",
                world.getDimension().getType().toString().replaceFirst("minecraft:", ""),
                section,
                world.isClient ? " (Client)" : "");
    }

    private static String getEntityString(World world, Entity e)
    {
        return String.format("%s.%s%s",
                world.getDimension().getType().toString().replaceFirst("minecraft:", ""),
                Registry.ENTITY_TYPE.getId(e.getType()).toString().replaceFirst("minecraft:", ""),
                world.isClient ? " (Client)" : "");
    }

    private static String getTEntityString(World world, BlockEntity be)
    {
        return String.format("%s.%s%s",
                world.getDimension().getType().toString().replaceFirst("minecraft:", ""),
                Registry.BLOCK_ENTITY.getId(be.getType()).toString().replaceFirst("minecraft:", ""),
                world.isClient ? " (Client)" : "");
    }

    public static void prepare_tick_report(ServerCommandSource source, int ticks)
    {
        //maybe add so it only spams the sending player, but honestly - all may want to see it
        time_repo.clear();
        test_type = TYPE.GENERAL;
        time_repo.put("tick", 0L);
        time_repo.put("Network", 0L);
        time_repo.put("Autosave", 0L);
        for (String section : GENERAL_SECTIONS)
        {
            time_repo.put(section, 0L);
        }
        for (String level : DIMENSIONS)
        {
            for (String section : SECTIONS)
            {
                time_repo.put(level + "." + section, 0L);
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
        time_repo.clear();
        time_repo.put("tick", 0L);
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
        return new ProfilerToken(type, name, world, System.nanoTime());
    }

    public static ProfilerToken start_entity_section(World world, Object e, TYPE type)
    {
        if (tick_health_requested == 0L || test_type != TYPE.ENTITY || current_tick_start == 0)
            return null;
        return new ProfilerToken(type, e, world, System.nanoTime());
    }

    public static void end_current_section(ProfilerToken tok)
    {
        if (tick_health_requested == 0L || test_type != TYPE.GENERAL || current_tick_start == 0)
            return;
        long end_time = System.nanoTime();
        if (tok.type == TYPE.GENERAL)
        {
            String current_section = (tok.world == null) ? (String) tok.section :
                    getSectionString(tok.world, (String)tok.section);
            if (time_repo.get(current_section) == null)
            {
                CarpetSettings.LOG.error("Current section: "+current_section);
            }
            else
                time_repo.put(current_section, time_repo.get(current_section) + end_time - tok.start);
        }
    }

    public static void end_current_entity_section(ProfilerToken tok)
    {
        if (tick_health_requested == 0L || test_type != TYPE.ENTITY || current_tick_start == 0)
            return;
        long end_time = System.nanoTime();
        String section;
        if (tok.type == TYPE.ENTITY)
            section = getEntityString(tok.world, (Entity) tok.section);
        else if (tok.type == TYPE.TILEENTITY)
            section = getTEntityString(tok.world, (BlockEntity) tok.section);
        else
            return;
        String time_section = "t." + section;
        String count_section = "c." + section;
        time_repo.put(time_section, time_repo.getOrDefault(time_section, 0L) + end_time - tok.start);
        time_repo.put(count_section, time_repo.getOrDefault(count_section, 0L) + 1);
    }

    public static void start_tick_profiling()
    {
        current_tick_start = System.nanoTime();
    }

    public static void end_tick_profiling(MinecraftServer server, ProfilerToken postTasks)
    {
        if (current_tick_start == 0L)
            return;
        if (postTasks != null)
            end_current_section(postTasks);
        time_repo.put("tick", time_repo.get("tick") + System.nanoTime() - current_tick_start);
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
        time_repo.clear();
        time_repo.put("tick", 0L);
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
        long total_tick_time = time_repo.get("tick");
        double divider = 1.0D / tick_health_requested / 1000000;
        Messenger.m(currentRequester, "w ");
        Messenger.m(currentRequester, String.format("gi Average tick time: %.3fms", divider * total_tick_time));
        long accumulated = 0L;

        for (String section : GENERAL_SECTIONS)
        {
            double amount = divider * time_repo.get(section);
            if (amount > 0.01)
            {
                accumulated += time_repo.get(section);
                Messenger.m(currentRequester, String.format("gi %s: %.3fms", section, amount));
            }
        }

        for (String dimension : DIMENSIONS)
        {
            boolean hasSomethin = false;
            for (String section : SECTIONS)
            {
                double amount = divider * time_repo.get(dimension + "." + section);
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
            Messenger.m(currentRequester, "gi "+dimension + ":");
            for (String section : SECTIONS)
            {
                double amount = divider * time_repo.get(dimension + "." + section);
                if (amount > 0.01)
                {
                    if (!(section.endsWith("(client)")))
                        accumulated += time_repo.get(dimension + "." + section);
                    Messenger.m(currentRequester, String.format("gi  - %s: %.3fms", section, amount));
                }
            }
        }

        long rest = total_tick_time - accumulated;

        Messenger.m(currentRequester, String.format("gi The Rest, whatever that might be: %.3fms", divider * rest));
    }

    public static void finalize_tick_report_for_entities(MinecraftServer server)
    {
        if (currentRequester == null)
            return;
        //print stats
        long total_tick_time = time_repo.get("tick");
        double divider = 1.0D / tick_health_requested / 1000000;
        double divider_1 = 1.0D / (tick_health_requested - 1) / 1000000;
        Messenger.m(currentRequester, "gi ");
        Messenger.m(currentRequester, String.format("gi Average tick time: %.3fms", divider * total_tick_time));
        time_repo.remove("tick");
        Messenger.m(currentRequester, "gi Top 10 counts:");
        int total = 0;
        for (Map.Entry<String, Long> entry : time_repo.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).collect(Collectors.toList()))
        {
            if (entry.getKey().startsWith("t."))
            {
                continue;
            }
            total++;
            if (total > 10)
            {
                continue;
            }
            String[] parts = entry.getKey().split("\\.");
            String dim = parts[1];
            String name = parts[2];
            int penalty = name.endsWith("(client)") ? 1 : 0;
            Messenger.m(currentRequester, String.format("gi  - %s in %s: %.1f", name, dim, 1.0D * entry.getValue() / (tick_health_requested - penalty)));
        }
        Messenger.m(currentRequester, "gi Top 10 CPU hogs:");
        total = 0;
        for (Map.Entry<String, Long> entry : time_repo.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).collect(Collectors.toList()))
        {
            if (entry.getKey().startsWith("c."))
            {
                continue;
            }
            total++;
            if (total > 10)
            {
                continue;
            }
            String[] parts = entry.getKey().split("\\.");
            String dim = parts[1];
            String name = parts[2];
            double applicableDivider = name.endsWith("(client)") ? divider : divider_1;
            Messenger.m(currentRequester, String.format("gi  - %s in %s: %.2fms", name, dim, applicableDivider * entry.getValue()));
        }
    }
}