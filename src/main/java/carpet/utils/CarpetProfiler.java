package carpet.utils;

import carpet.CarpetSettings;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.registry.Registry;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class CarpetProfiler
{
    private static final HashMap<String, Long> time_repo = new HashMap<>();
    public static int tick_health_requested = 0;
    private static int tick_health_elapsed = 0;
    private static int test_type = 0; //1 for ticks, 2 for entities;
    private static String current_section = null;
    private static long current_section_start = 0;
    private static long current_tick_start = 0;
    private static String [] GENERAL_SECTIONS = {"Network", "Autosave"};
    private static String [] DIMENSIONS = {"Overworld","The End","The Nether"};
    private static String [] SECTIONS = {"Spawning","Blocks","Entities","Tile Entities","Entities(client)","Tile Entities(client)","Villages"};

    public static void prepare_tick_report(int ticks)
    {
        //maybe add so it only spams the sending player, but honestly - all may want to see it
        time_repo.clear();
        test_type = 1;
        time_repo.put("tick",0L);
        time_repo.put("Network",0L);
        time_repo.put("Autosave",0L);
        for (String section: GENERAL_SECTIONS)
        {
            time_repo.put(section,0L);
        }
        for (String level: DIMENSIONS)
        {
            for (String section: SECTIONS)
            {
                time_repo.put(level+"."+section,0L);
            }
        }

        tick_health_elapsed = ticks;
        tick_health_requested = ticks;
        current_tick_start = 0L;
        current_section_start = 0L;
        current_section = null;

    }

    public static void start_section(String dimension, String name)
    {
        if (tick_health_requested == 0L || test_type != 1)
        {
            return;
        }
        if (current_tick_start == 0L)
        {
            return;
        }
        if (current_section != null)
        {
            end_current_section();
        }
        String key = name;
        if (dimension != null)
        {
            key = dimension+"."+name;
        }
        current_section = key;
        current_section_start = System.nanoTime();
    }


    public static class ProfilerToken
    {
        public String section;
        public long start;
        public ProfilerToken(String name, long start)
        {
            this.section = name;
            this.start = start;
        }
    }

    public static ProfilerToken start_section_concurrent(String dimension, String name, boolean isRemote)
    {
        if (tick_health_requested == 0L || test_type != 1)
        {
            return null;
        }
        if (current_tick_start == 0L)
        {
            return null;
        }

        String key = name;
        if (dimension != null)
        {
            key = dimension+"."+name;
        }
        if (isRemote) key += "(client)";
        long time = System.nanoTime();
        return new ProfilerToken(key, time);
    }

    public static ProfilerToken start_entity_section(String dimension, Entity e)
    {
        if (tick_health_requested == 0L || test_type != 2)
        {
            return null;
        }
        if (current_tick_start == 0L)
        {
            return null;
        }
        String section = dimension+"."+ Registry.ENTITY_TYPE.getId(e.getType()).toString().replaceFirst("minecraft:","");
        if (e.getWorld().isClient)
            section += "(client)";
        long section_start = System.nanoTime();
        return new ProfilerToken(section, section_start);
    }

    public static ProfilerToken start_tileentity_section(String dimension, BlockEntity e)
    {
        if (tick_health_requested == 0L || test_type != 2)
        {
            return null;
        }
        if (current_tick_start == 0L)
        {
            return null;
        }
        String section = dimension+"."+ BlockEntityType.getId(e.getType()).toString().replaceFirst("minecraft:","");
        if (e.getWorld() == null)
            section += "??";
        else if (e.getWorld().isClient)
            section += "(client)";
        long section_start = System.nanoTime();
        return new ProfilerToken(section, section_start);
    }

    public static void end_current_section()
    {
        if (tick_health_requested == 0L || test_type != 1)
        {
            return;
        }
        long end_time = System.nanoTime();
        if (current_tick_start == 0L)
        {
            return;
        }
        if (current_section == null)
        {
            CarpetSettings.LOG.error("finishing section that hasn't started");
            return;
        }
        //CarpetSettings.LOG.error("finishing section "+current_section);
        time_repo.put(current_section,time_repo.get(current_section)+end_time-current_section_start);
        current_section = null;
        current_section_start = 0;
    }

    public static void end_current_section_concurrent(ProfilerToken tok)
    {
        if (tick_health_requested == 0L || test_type != 1)
        {
            return;
        }
        long end_time = System.nanoTime();
        if (current_tick_start == 0L)
        {
            return;
        }
        if (tok == null)
        {
            CarpetSettings.LOG.error("finishing section that hasn't started");
            return;
        }
        //CarpetSettings.LOG.error("finishing section "+current_section);
        time_repo.put(tok.section,time_repo.get(tok.section)+end_time-tok.start);
    }

    public static void end_current_entity_section(ProfilerToken tok)
    {
        if (tick_health_requested == 0L || test_type != 2)
        {
            return;
        }
        long end_time = System.nanoTime();
        if (current_tick_start == 0L)
        {
            return;
        }
        if (tok == null)
        {
            CarpetSettings.LOG.error("finishing entity/TE section that hasn't started");
            return;
        }
        String time_section = "t."+tok.section;
        String count_section = "c."+tok.section;
        time_repo.put(time_section,time_repo.getOrDefault(time_section,0L)+end_time-tok.start);
        time_repo.put(count_section,time_repo.getOrDefault(count_section,0L)+1);
    }

    public static void start_tick_profiling()
    {
        current_tick_start = System.nanoTime();
    }

    public static void end_tick_profiling(MinecraftServer server)
    {
        if (current_tick_start == 0L)
        {
            return;
        }
        time_repo.put("tick",time_repo.get("tick")+System.nanoTime()-current_tick_start);
        tick_health_elapsed --;
        //CarpetSettings.LOG.error("tick count current at "+time_repo.get("tick"));
        if (tick_health_elapsed <= 0)
        {
            finalize_tick_report(server);
        }
    }

    public static void finalize_tick_report(MinecraftServer server)
    {
        if (test_type == 1)
        {
            finalize_tick_report_for_time(server);
        }
        if (test_type == 2)
        {
            finalize_tick_report_for_entities(server);
        }
        cleanup_tick_report();
    }

    public static void cleanup_tick_report()
    {
        time_repo.clear();
        time_repo.put("tick",0L);
        test_type = 0;
        tick_health_elapsed = 0;
        tick_health_requested = 0;
        current_tick_start = 0L;
        current_section_start = 0L;
        current_section = null;

    }

    public static void finalize_tick_report_for_time(MinecraftServer server)
    {
        //print stats
        long total_tick_time = time_repo.get("tick");
        double divider = 1.0D/tick_health_requested/1000000;
        Messenger.print_server_message(server, String.format("Average tick time: %.3fms",divider*total_tick_time));
        long accumulated = 0L;

        for (String section: GENERAL_SECTIONS)
        {
            double amount = divider*time_repo.get(section);
            if (amount > 0.01)
            {
                accumulated += time_repo.get(section);
                Messenger.print_server_message(server, String.format("%s: %.3fms", section, amount));
            }
        }

        for (String dimension: DIMENSIONS)
        {
            boolean hasSomethin = false;
            for (String section: SECTIONS)
            {
                double amount = divider*time_repo.get(dimension+"."+section);
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
            Messenger.print_server_message(server, dimension+":");
            for (String section: SECTIONS)
            {
                double amount = divider*time_repo.get(dimension+"."+section);
                if (amount > 0.01)
                {
                    if (!(section.endsWith("(client)")))
                        accumulated += time_repo.get(dimension+"."+section);
                    Messenger.print_server_message(server, String.format(" - %s: %.3fms", section, amount));
                }
            }
        }

        long rest = total_tick_time-accumulated;

        Messenger.print_server_message(server, String.format("The Rest, whatever that might be: %.3fms",divider*rest));
    }

    public static void finalize_tick_report_for_entities(MinecraftServer server)
    {
        //print stats
        long total_tick_time = time_repo.get("tick");
        double divider = 1.0D/tick_health_requested/1000000;
        double divider_1 = 1.0D/(tick_health_requested-1)/1000000;
        Messenger.print_server_message(server, String.format("Average tick time: %.3fms",divider*total_tick_time));
        time_repo.remove("tick");
        Messenger.print_server_message(server, "Top 10 counts:");
        int total = 0;
        for ( Map.Entry<String, Long> entry : time_repo.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).collect(Collectors.toList()) )
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
            int penalty = name.endsWith("(client)") ? 1 :0;
            Messenger.print_server_message(server, String.format(" - %s in %s: %.3f",name, dim, 1.0D*entry.getValue()/(tick_health_requested-penalty)));
        }
        Messenger.print_server_message(server, "Top 10 grossing:");
        total = 0;
        for ( Map.Entry<String, Long> entry : time_repo.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).collect(Collectors.toList()) )
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
            double applicableDivider = name.endsWith("(client)") ? divider :divider_1;
            Messenger.print_server_message(server, String.format(" - %s in %s: %.3fms",name, dim, applicableDivider*entry.getValue()));
        }

    }

    public static void prepare_entity_report(int ticks)
    {
        //maybe add so it only spams the sending player, but honestly - all may want to see it
        time_repo.clear();
        time_repo.put("tick",0L);
        test_type = 2;
        tick_health_elapsed = ticks;
        tick_health_requested = ticks;
        current_tick_start = 0L;
        current_section_start = 0L;
        current_section = null;

    }
}
