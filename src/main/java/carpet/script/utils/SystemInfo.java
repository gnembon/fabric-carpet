package carpet.script.utils;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.script.CarpetContext;
import carpet.script.CarpetScriptHost;
import carpet.script.value.BooleanValue;
import carpet.script.value.ListValue;
import carpet.script.value.MapValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import carpet.script.value.ValueConversions;
import carpet.settings.CarpetRule;
import carpet.settings.RuleHelper;
import carpet.settings.SettingsManager;
import com.sun.management.OperatingSystemMXBean;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.SharedConstants;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.GameRules;
import net.minecraft.world.WorldProperties;

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
        put("app_list", c -> ListValue.wrap(((CarpetScriptHost)c.host).getScriptServer().modules.keySet().stream().filter(Objects::nonNull).map(StringValue::new).collect(Collectors.toList())));
        put("app_scope", c -> StringValue.of((c.host).isPerUser()?"player":"global"));
        put("app_players", c -> ListValue.wrap(c.host.getUserList().stream().map(StringValue::new).collect(Collectors.toList())));

        put("world_name", c -> new StringValue(c.s.getServer().getSaveProperties().getLevelName()));
        put("world_seed", c -> new NumericValue(c.s.getWorld().getSeed()));
        put("server_motd", c -> StringValue.of(c.s.getServer().getServerMotd()));
        put("world_path", c -> StringValue.of(c.s.getServer().getSavePath(WorldSavePath.ROOT).toString()));
        put("world_folder", c -> {
            Path serverPath = c.s.getServer().getSavePath(WorldSavePath.ROOT);
            int nodeCount = serverPath.getNameCount();
            if (nodeCount < 2) return Value.NULL;
            String tlf = serverPath.getName(nodeCount-2).toString();
            return StringValue.of(tlf);
        });
        put("world_dimensions", c -> ListValue.wrap(c.s.getServer().getWorldRegistryKeys().stream().map(k -> ValueConversions.of(k.getValue())).collect(Collectors.toList())));
        put("world_spawn_point", c -> {
            WorldProperties prop = c.s.getServer().getOverworld().getLevelProperties();
            return ListValue.of(NumericValue.of(prop.getSpawnX()), NumericValue.of(prop.getSpawnY()), NumericValue.of(prop.getSpawnZ()));
        });
        put("world_time", c -> new NumericValue(c.s.getWorld().getTime()));

        put("game_difficulty", c -> StringValue.of(c.s.getServer().getSaveProperties().getDifficulty().getName()));
        put("game_hardcore", c -> BooleanValue.of(c.s.getServer().getSaveProperties().isHardcore()));
        put("game_storage_format", c -> StringValue.of(c.s.getServer().getSaveProperties().getFormatName(c.s.getServer().getSaveProperties().getVersion())));
        put("game_default_gamemode", c -> StringValue.of(c.s.getServer().getDefaultGameMode().getName()));
        put("game_max_players", c -> new NumericValue(c.s.getServer().getMaxPlayerCount()));
        put("game_view_distance", c -> new NumericValue(c.s.getServer().getPlayerManager().getViewDistance()));
        put("game_mod_name", c -> StringValue.of(c.s.getServer().getServerModName()));
        put("game_version", c -> StringValue.of(c.s.getServer().getVersion()));
        put("game_target", c -> StringValue.of(SharedConstants.getGameVersion().getReleaseTarget()));
        put("game_protocol", c -> NumericValue.of(SharedConstants.getProtocolVersion()));
        put("game_major_target", c -> {
            String [] vers = SharedConstants.getGameVersion().getReleaseTarget().split("\\.");
            return NumericValue.of((vers.length > 1)?Integer.parseInt(vers[1]):0);
        });
        put("game_minor_target", c -> {
            String [] vers = SharedConstants.getGameVersion().getReleaseTarget().split("\\.");
            return NumericValue.of((vers.length > 2)?Integer.parseInt(vers[2]):0);
        });
        put("game_stable", c -> BooleanValue.of(SharedConstants.getGameVersion().isStable()));
        put("game_data_version", c->NumericValue.of(SharedConstants.getGameVersion().getWorldVersion()));
        put("game_pack_version", c->NumericValue.of(SharedConstants.getGameVersion().getPackVersion()));

        put("server_ip", c -> StringValue.of(c.s.getServer().getServerIp()));
        put("server_whitelisted", c -> BooleanValue.of(c.s.getServer().isEnforceWhitelist()));
        put("server_whitelist", c -> {
            MapValue whitelist = new MapValue(Collections.emptyList());
            for (String s: c.s.getServer().getPlayerManager().getWhitelistedNames())
            {
                whitelist.append(StringValue.of(s));
            }
            return whitelist;
        });
        put("server_banned_players", c -> {
            MapValue whitelist = new MapValue(Collections.emptyList());
            for (String s: c.s.getServer().getPlayerManager().getUserBanList().getNames())
            {
                whitelist.append(StringValue.of(s));
            }
            return whitelist;
        });
        put("server_banned_ips", c -> {
            MapValue whitelist = new MapValue(Collections.emptyList());
            for (String s: c.s.getServer().getPlayerManager().getIpBanList().getNames())
            {
                whitelist.append(StringValue.of(s));
            }
            return whitelist;
        });
        put("server_dev_environment", c-> BooleanValue.of(FabricLoader.getInstance().isDevelopmentEnvironment()));
        put("server_mods", c -> {
            Map<Value, Value> ret = new HashMap<>();
            for (ModContainer mod : FabricLoader.getInstance().getAllMods())
                ret.put(new StringValue(mod.getMetadata().getName()), new StringValue(mod.getMetadata().getVersion().getFriendlyString()));
            return MapValue.wrap(ret);
        });
        put("server_last_tick_times", c -> {
        	//assuming we are in the tick world section
            // might be off one tick when run in the off tasks or asynchronously.
            int currentReportedTick = c.s.getServer().getTicks()-1;
            List<Value> ticks = new ArrayList<>(100);
            final long[] tickArray = c.s.getServer().lastTickLengths;
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
            Map<Value, Value> carpetRules = new HashMap<Value, Value>();
            for (CarpetRule<?> rule : CarpetServer.settingsManager.getCarpetRules())
                carpetRules.put(new StringValue(rule.name()), new StringValue(RuleHelper.toRuleString(rule.value())));
            CarpetServer.extensions.forEach(e -> {
                SettingsManager manager = e.customSettingsManager();
                if (manager == null) return;

                for (CarpetRule<?> rule: manager.getCarpetRules())
                    carpetRules.put(new StringValue(manager.getIdentifier() + ":" + rule.name()), new StringValue(RuleHelper.toRuleString(rule.value())));
            });
            return MapValue.wrap(carpetRules);
        });
        put("world_gamerules", c -> {
            Map<Value, Value> rules = new HashMap<>();
            final GameRules gameRules = c.s.getWorld().getGameRules();
            GameRules.accept(new GameRules.Visitor() {
                @Override
                public <T extends GameRules.Rule<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
                    rules.put(StringValue.of(key.getName()), StringValue.of(gameRules.get(key).toString()));
                }
            });
            return MapValue.wrap(rules);
        });
        put("scarpet_version", c -> StringValue.of(CarpetSettings.carpetVersion));

    }};
    public static Value get(String what, CarpetContext cc)
    {
        return options.getOrDefault(what, c -> null).apply(cc);
    }
    public static Value getAll(CarpetContext cc)
    {
        return MapValue.wrap(options.entrySet().stream().collect(Collectors.toMap(e -> new StringValue(e.getKey()), e -> e.getValue().apply(cc))));
    }

}
