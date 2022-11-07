package carpet.script.utils;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.RuleHelper;
import carpet.script.CarpetContext;
import carpet.script.CarpetScriptHost;
import carpet.script.value.BooleanValue;
import carpet.script.value.EntityValue;
import carpet.script.value.ListValue;
import carpet.script.value.MapValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import carpet.script.value.ValueConversions;
import carpet.api.settings.SettingsManager;
import com.mojang.bridge.game.PackType;
import com.sun.management.OperatingSystemMXBean;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.SharedConstants;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec2;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SystemInfo {
    private static final Map<String, Function<CarpetContext,Value>> options = new HashMap<String, Function<CarpetContext,Value>>(){{
        put("app_name", c ->
        {
            String name = c.host.getName();
            return name == null?Value.NULL:new StringValue(name);
        });
        put("app_list", c -> ListValue.wrap(((CarpetScriptHost)c.host).scriptServer().modules.keySet().stream().filter(Objects::nonNull).map(StringValue::new).collect(Collectors.toList())));
        put("app_scope", c -> StringValue.of((c.host).isPerUser()?"player":"global"));
        put("app_players", c -> ListValue.wrap(c.host.getUserList().stream().map(StringValue::new).collect(Collectors.toList())));

        put("world_name", c -> new StringValue(c.s.getServer().getWorldData().getLevelName()));
        put("world_seed", c -> new NumericValue(c.s.getLevel().getSeed()));
        put("server_motd", c -> StringValue.of(c.s.getServer().getMotd()));
        put("world_path", c -> StringValue.of(c.s.getServer().getWorldPath(LevelResource.ROOT).toString()));
        put("world_folder", c -> {
            Path serverPath = c.s.getServer().getWorldPath(LevelResource.ROOT);
            int nodeCount = serverPath.getNameCount();
            if (nodeCount < 2) return Value.NULL;
            String tlf = serverPath.getName(nodeCount-2).toString();
            return StringValue.of(tlf);
        });
        put("world_dimensions", c -> ListValue.wrap(c.s.getServer().levelKeys().stream().map(k -> ValueConversions.of(k.location())).collect(Collectors.toList())));
        put("world_spawn_point", c -> {
            LevelData prop = c.s.getServer().overworld().getLevelData();
            return ListValue.of(NumericValue.of(prop.getXSpawn()), NumericValue.of(prop.getYSpawn()), NumericValue.of(prop.getZSpawn()));
        });

        put("world_bottom", c-> new NumericValue(c.s.getLevel().getMinBuildHeight()));

        put("world_top", c-> new NumericValue(c.s.getLevel().getMaxBuildHeight()));

        put("world_center", c-> {
            WorldBorder worldBorder = c.s.getLevel().getWorldBorder();
            return ListValue.fromTriple(worldBorder.getCenterX(), 0, worldBorder.getCenterZ());
        });

        put("world_size", c -> new NumericValue(c.s.getLevel().getWorldBorder().getSize() / 2));

        put("world_max_size", c-> new NumericValue( c.s.getLevel().getWorldBorder().getAbsoluteMaxSize()));

        put("world_time", c -> new NumericValue(c.s.getLevel().getGameTime()));

        put("game_difficulty", c -> StringValue.of(c.s.getServer().getWorldData().getDifficulty().getKey()));
        put("game_hardcore", c -> BooleanValue.of(c.s.getServer().getWorldData().isHardcore()));
        put("game_storage_format", c -> StringValue.of(c.s.getServer().getWorldData().getStorageVersionName(c.s.getServer().getWorldData().getVersion())));
        put("game_default_gamemode", c -> StringValue.of(c.s.getServer().getDefaultGameType().getName()));
        put("game_max_players", c -> new NumericValue(c.s.getServer().getMaxPlayers()));
        put("game_view_distance", c -> new NumericValue(c.s.getServer().getPlayerList().getViewDistance()));
        put("game_mod_name", c -> StringValue.of(c.s.getServer().getServerModName()));
        put("game_version", c -> StringValue.of(c.s.getServer().getServerVersion()));
        put("game_target", c -> StringValue.of(CarpetSettings.releaseTarget));
        put("game_protocol", c -> NumericValue.of(SharedConstants.getProtocolVersion()));
        put("game_major_target", c -> {
            String [] vers = CarpetSettings.releaseTarget.split("\\.");
            return NumericValue.of((vers.length > 1)?Integer.parseInt(vers[1]):0);
        });
        put("game_minor_target", c -> {
            String [] vers = CarpetSettings.releaseTarget.split("\\.");
            return NumericValue.of((vers.length > 2)?Integer.parseInt(vers[2]):0);
        });
        put("game_stable", c -> BooleanValue.of(SharedConstants.getCurrentVersion().isStable()));
        put("game_data_version", c->NumericValue.of(SharedConstants.getCurrentVersion().getWorldVersion()));
        put("game_pack_version", c->NumericValue.of(SharedConstants.getCurrentVersion().getPackVersion(PackType.DATA)));

        put("server_ip", c -> StringValue.of(c.s.getServer().getLocalIp()));
        put("server_whitelisted", c -> BooleanValue.of(c.s.getServer().isEnforceWhitelist()));
        put("server_whitelist", c -> {
            MapValue whitelist = new MapValue(Collections.emptyList());
            for (String s: c.s.getServer().getPlayerList().getWhiteListNames())
            {
                whitelist.append(StringValue.of(s));
            }
            return whitelist;
        });
        put("server_banned_players", c -> {
            MapValue whitelist = new MapValue(Collections.emptyList());
            for (String s: c.s.getServer().getPlayerList().getBans().getUserList())
            {
                whitelist.append(StringValue.of(s));
            }
            return whitelist;
        });
        put("server_banned_ips", c -> {
            MapValue whitelist = new MapValue(Collections.emptyList());
            for (String s: c.s.getServer().getPlayerList().getIpBans().getUserList())
            {
                whitelist.append(StringValue.of(s));
            }
            return whitelist;
        });
        put("server_dev_environment", c-> BooleanValue.of(FabricLoader.getInstance().isDevelopmentEnvironment()));
        put("server_mods", c -> {
            Map<Value, Value> ret = new HashMap<>();
            for (ModContainer mod : FabricLoader.getInstance().getAllMods())
                ret.put(new StringValue(mod.getMetadata().getId()), new StringValue(mod.getMetadata().getVersion().getFriendlyString()));
            return MapValue.wrap(ret);
        });
        put("server_last_tick_times", c -> {
        	//assuming we are in the tick world section
            // might be off one tick when run in the off tasks or asynchronously.
            int currentReportedTick = c.s.getServer().getTickCount()-1;
            List<Value> ticks = new ArrayList<>(100);
            final long[] tickArray = c.s.getServer().tickTimes;
            for (int i=currentReportedTick+100; i > currentReportedTick; i--)
            {
                ticks.add(new NumericValue(((double)tickArray[i % 100])/1000000.0));
            }
            return ListValue.wrap(ticks);
        });

        put("java_max_memory", c -> new NumericValue(Runtime.getRuntime().maxMemory()));
        put("java_allocated_memory", c -> new NumericValue(Runtime.getRuntime().totalMemory()));
        put("java_used_memory", c -> new NumericValue(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()));
        put("java_cpu_count", c -> new NumericValue(Runtime.getRuntime().availableProcessors()));
        put("java_version", c -> StringValue.of(System.getProperty("java.version")));
        put("java_bits", c -> {
            for (String property : new String[]{"sun.arch.data.model", "com.ibm.vm.bitmode", "os.arch"})
            {
                String value = System.getProperty(property);
                if (value != null && value.contains("64"))
                    return new NumericValue(64);

            }
            return new NumericValue(32);
        });
        put("java_system_cpu_load", c -> {
            OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(
                    OperatingSystemMXBean.class);
            return new NumericValue(osBean.getCpuLoad());
        });
        put("java_process_cpu_load", c -> {
            OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(
                    OperatingSystemMXBean.class);
            return new NumericValue(osBean.getProcessCpuLoad());
        });
        put("world_carpet_rules", c -> {
            Collection<CarpetRule<?>> rules = CarpetServer.settingsManager.getCarpetRules();
            MapValue carpetRules = new MapValue(Collections.emptyList());
            rules.forEach(rule -> {
                carpetRules.put(new StringValue(rule.name()), new StringValue(RuleHelper.toRuleString(rule.value())));
            });
            CarpetServer.extensions.forEach(e -> {
                SettingsManager manager = e.extensionSettingsManager();
                if (manager == null) return;

                Collection<CarpetRule<?>> extensionRules = manager.getCarpetRules();
                extensionRules.forEach(rule -> {
                    carpetRules.put(new StringValue(manager.identifier()+":"+rule.name()), new StringValue(RuleHelper.toRuleString(rule.value())));
                });
            });
            return carpetRules;
        });
        put("world_gamerules", c->{
            Map<Value, Value> rules = new HashMap<>();
            final GameRules gameRules = c.s.getLevel().getGameRules();
            GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
                @Override
                public <T extends GameRules.Value<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
                    rules.put(StringValue.of(key.getId()), StringValue.of(gameRules.getRule(key).toString()));
                }
            });
            return MapValue.wrap(rules);
        });

        put("source_entity", c -> EntityValue.of(c.s.getEntity()));
        put("source_position", c -> ValueConversions.of(c.s.getPosition()));
        put("source_dimension", c -> ValueConversions.of(c.s.getLevel()));
        put("source_rotation", c -> {
            Vec2 rotation = c.s.getRotation();
            return ListValue.of(new NumericValue(rotation.x), new NumericValue(rotation.y));
        });
        
        put("scarpet_version", c -> StringValue.of(CarpetSettings.carpetVersion));

    }};
    public static Value get(String what, CarpetContext cc)
    {
        return options.getOrDefault(what, c -> null).apply(cc);
    }
    public static Value getAll()
    {
        return ListValue.wrap(options.keySet().stream().map(StringValue::of).collect(Collectors.toList()));
    }

}
