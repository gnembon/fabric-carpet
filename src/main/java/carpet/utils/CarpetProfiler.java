package carpet.utils;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.BaseComponent;
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
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class CarpetProfiler
{
    private static final Map<String, Long> SECTION_STATS = new HashMap<>();
    private static final Object2LongMap<Pair<Level,Object>> ENTITY_TIMES = new Object2LongOpenHashMap<>();
    private static final Object2LongMap<Pair<Level,Object>> ENTITY_COUNT = new Object2LongOpenHashMap<>();


    private static CommandSourceStack currentRequester = null;
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
        for (ResourceKey<Level> level : source.getServer().levelKeys())
        {
            for (String section : SECTIONS)
            {
                SECTION_STATS.put(level.location() + "." + section, 0L);
            }
        }

        tick_health_elapsed = ticks;
        tick_health_requested = ticks;
        current_tick_start = 0L;
        currentRequester = source;
    }

    public static void prepare_entity_report(CommandSourceStack source, int ticks)
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
            SECTION_STATS.put(current_section, SECTION_STATS.getOrDefault(current_section, 0L) + end_time - tok.start);
        }
    }

    public static void end_current_entity_section(ProfilerToken tok)
    {
        if (tick_health_requested == 0L || test_type != TYPE.ENTITY || current_tick_start == 0 || tok == null)
            return;
        long end_time = System.nanoTime();
        Pair<Level,Object> section = Pair.of(tok.world, tok.section);
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

    private static BaseComponent sectionName(String section)
    {
        String key = "carpet.command.tick.section." + section.replace(' ', '_').replace("(", "").replace(")", "").toLowerCase();
        if (currentRequester != null && Translations.hasTranslation(key, currentRequester))
        {
            return Messenger.tr(key);
        }
        return Messenger.s(section);
    }

    public static void finalize_tick_report_for_time(MinecraftServer server)
    {
        //print stats
        if (currentRequester == null)
            return;
        long total_tick_time = SECTION_STATS.get("tick");
        double divider = 1.0D / tick_health_requested / 1000000;
        Messenger.m(currentRequester, "w ");
        Messenger.m(currentRequester, "wb", Messenger.tr("carpet.command.tick.report.average_tick_time", Messenger.c(String.format("yb %.3fms", divider * total_tick_time))));

        long accumulated = 0L;

        for (String section : GENERAL_SECTIONS)
        {
            double amount = divider * SECTION_STATS.get(section);
            if (amount > 0.01)
            {
                accumulated += SECTION_STATS.get(section);
                Messenger.m(currentRequester, Messenger.c(" - ", sectionName(section)), "w : ", String.format("y %.3fms", amount));
            }
        }
        for (String section : SCARPET_SECTIONS)
        {
            double amount = divider * SECTION_STATS.get(section);
            if (amount > 0.01)
            {
                Messenger.m(currentRequester, "gi", Messenger.c(" - ", sectionName(section)), "w : ", String.format("di %.3fms", amount));
            }
        }

        for (ResourceKey<Level> dim : server.levelKeys())
        {
            ResourceLocation dimensionId = dim.location();
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
            Messenger.m(currentRequester, "wb", Messenger.dim(dim), "wb :");
            for (String section : SECTIONS)
            {
                double amount = divider * SECTION_STATS.getOrDefault(dimensionId + "." + section, 0L);
                if (amount > 0.01)
                {
                    boolean cli = section.endsWith("(Client)");
                    if (!cli)
                        accumulated += SECTION_STATS.get(dimensionId + "." + section);
                    Messenger.m(currentRequester, cli?"gi":"w", Messenger.c(" - ", sectionName(section)), "w : ", String.format("%s %.3fms", cli?"di":"y", amount));
                }
            }
        }

        long rest = total_tick_time - accumulated;

        Messenger.m(currentRequester, "gi", Messenger.tr("carpet.command.tick.report.the_rest", String.format("%.3f", divider * rest)));
    }

    private static BaseComponent sectionName(Pair<Level,Object> section)
    {
        BaseComponent name;
        if (section.getValue() instanceof EntityType entityType)
        {
            name = (BaseComponent)entityType.getDescription();
        }
        else
        {
            ResourceLocation id = Registry.BLOCK_ENTITY_TYPE.getKey((BlockEntityType<?>) section.getValue());
            name = Messenger.s("minecraft".equals(id.getNamespace())?id.getPath():id.toString());
        }
        if (section.getKey().isClientSide)
        {
            name.append(" (").append(Messenger.tr("carpet.command.tick.section_name.client")).append(")");
        }
        return Messenger.tr("carpet.command.tick.section_name.text", name, Messenger.dim(section.getKey().dimension()));
    }

    public static void finalize_tick_report_for_entities(MinecraftServer server)
    {
        if (currentRequester == null)
            return;
        long total_tick_time = SECTION_STATS.get("tick");
        double divider = 1.0D / tick_health_requested / 1000000;
        double divider_1 = 1.0D / (tick_health_requested - 1) / 1000000;
        Messenger.m(currentRequester, "w ");
        Messenger.m(currentRequester, "wb", Messenger.tr("carpet.command.tick.report.average_tick_time", Messenger.c(String.format("yb %.3fms", divider * total_tick_time))));
        SECTION_STATS.remove("tick");
        int maxEntry = 10;
        int total = 0;
        Messenger.m(currentRequester, "wb", Messenger.tr("carpet.command.tick.report.top_n_count", maxEntry));
        for (Object2LongMap.Entry<Pair<Level, Object>> sectionEntry : ENTITY_COUNT.object2LongEntrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).collect(Collectors.toList()))
        {
            if (++total > maxEntry) break;
            Pair<Level,Object> section = sectionEntry.getKey();
            boolean cli = section.getKey().isClientSide;
            Messenger.m(currentRequester,
                    cli? "gi": "w", Messenger.c(" - ", sectionName(section), " : "),
                    String.format("%s %.1f", cli?"di":"y",
                    1.0D * sectionEntry.getLongValue() / (tick_health_requested - (cli? 1 : 0))
            ));
        }
        Messenger.m(currentRequester, "wb", Messenger.tr("carpet.command.tick.report.top_n_cpu_hog", maxEntry));
        total = 0;
        for (Object2LongMap.Entry<Pair<Level,Object>> sectionEntry : ENTITY_TIMES.object2LongEntrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).collect(Collectors.toList()))
        {
            if (++total > maxEntry) break;
            Pair<Level,Object> section = sectionEntry.getKey();
            boolean cli = section.getKey().isClientSide;
            Messenger.m(currentRequester,
                    cli? "gi": "w", Messenger.c(" - ", sectionName(section), " : "),
                    String.format("%s %.2fms", cli?"di":"y",
                    (cli ? divider : divider_1) * sectionEntry.getLongValue()
            ));
        }
    }
}