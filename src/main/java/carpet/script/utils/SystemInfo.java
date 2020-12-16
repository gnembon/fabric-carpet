package carpet.script.utils;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.script.CarpetContext;
import carpet.script.CarpetScriptHost;
import carpet.script.value.ListValue;
import carpet.script.value.MapValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import carpet.settings.ParsedRule;
import carpet.settings.SettingsManager;
import com.sun.management.OperatingSystemMXBean;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.GameRules;

import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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

        put("world_name", c -> new StringValue(c.s.getMinecraftServer().getSaveProperties().getLevelName()));
        put("world_seed", c -> new NumericValue(c.s.getWorld().getSeed()));
        put("server_motd", c -> StringValue.of(c.s.getMinecraftServer().getServerMotd()));
        put("world_path", c -> StringValue.of(c.s.getMinecraftServer().getSavePath(WorldSavePath.ROOT).toString()));
        put("world_folder", c -> {
            Path serverPath = c.s.getMinecraftServer().getSavePath(WorldSavePath.ROOT);
            int nodeCount = serverPath.getNameCount();
            if (nodeCount < 2) return Value.NULL;
            String tlf = serverPath.getName(nodeCount-2).toString();
            return StringValue.of(tlf);
        });
        put("game_difficulty", c -> StringValue.of(c.s.getMinecraftServer().getSaveProperties().getDifficulty().getName()));
        put("game_hardcore", c -> new NumericValue(c.s.getMinecraftServer().getSaveProperties().isHardcore()));
        put("game_storage_format", c -> StringValue.of(c.s.getMinecraftServer().getSaveProperties().getFormatName(c.s.getMinecraftServer().getSaveProperties().getVersion())));
        put("game_default_gamemode", c -> StringValue.of(c.s.getMinecraftServer().getDefaultGameMode().getName()));
        put("game_max_players", c -> new NumericValue(c.s.getMinecraftServer().getMaxPlayerCount()));
        put("game_view_distance", c -> new NumericValue(c.s.getMinecraftServer().getPlayerManager().getViewDistance()));
        put("game_mod_name", c -> StringValue.of(c.s.getMinecraftServer().getServerModName()));
        put("game_version", c -> StringValue.of(c.s.getMinecraftServer().getVersion()));

        put("server_ip", c -> StringValue.of(c.s.getMinecraftServer().getServerIp()));
        put("server_whitelisted", c -> new NumericValue(c.s.getMinecraftServer().isEnforceWhitelist()));
        put("server_whitelist", c -> {
            MapValue whitelist = new MapValue(Collections.emptyList());
            for (String s: c.s.getMinecraftServer().getPlayerManager().getWhitelistedNames())
            {
                whitelist.append(StringValue.of(s));
            }
            return whitelist;
        });
        put("server_banned_players", c -> {
            MapValue whitelist = new MapValue(Collections.emptyList());
            for (String s: c.s.getMinecraftServer().getPlayerManager().getUserBanList().getNames())
            {
                whitelist.append(StringValue.of(s));
            }
            return whitelist;
        });
        put("server_banned_ips", c -> {
            MapValue whitelist = new MapValue(Collections.emptyList());
            for (String s: c.s.getMinecraftServer().getPlayerManager().getIpBanList().getNames())
            {
                whitelist.append(StringValue.of(s));
            }
            return whitelist;
        });
        put("server_dev_environment", c-> new NumericValue(FabricLoader.getInstance().isDevelopmentEnvironment()));

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
            return new NumericValue(osBean.getSystemCpuLoad());
        });
        put("java_process_cpu_load", c -> {
            OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(
                    OperatingSystemMXBean.class);
            return new NumericValue(osBean.getProcessCpuLoad());
        });
        put("world_carpet_rules", c -> {
            Collection<ParsedRule<?>> rules = CarpetServer.settingsManager.getRules();
            MapValue carpetRules = new MapValue(Collections.emptyList());
            rules.forEach(rule -> {
                carpetRules.put(new StringValue(rule.name), new StringValue(rule.getAsString()));
            });
            CarpetServer.extensions.forEach(e -> {
                SettingsManager manager = e.customSettingsManager();
                if (manager == null) return;

                Collection<ParsedRule<?>> extensionRules = manager.getRules();
                extensionRules.forEach(rule -> {
                    carpetRules.put(new StringValue(manager.getIdentifier()+":"+rule.name), new StringValue(rule.getAsString()));
                });
            });
            return carpetRules;
        });
        put("world_gamerules", c->{
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
