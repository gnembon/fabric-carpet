package carpet.settings;

import carpet.CarpetSettings;
import com.google.common.collect.ImmutableList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SettingsManager {
    private static final Logger LOG = LogManager.getLogger();
    private static Map<String, ParsedRule<?>> rules = new HashMap<>();
    public boolean locked;
    MinecraftServer server;

    public SettingsManager(MinecraftServer server)
    {
        this.server = server;
        loadConfigurationFromConf();
    }


    public static void parseSettingsClass(Class settingsClass)
    {
        for (Field f : settingsClass.getDeclaredFields())
        {
            Rule rule = f.getAnnotation(Rule.class);
            if (rule == null) continue;
            ParsedRule parsed = new ParsedRule(f, rule);
            CarpetSettings.LOG.error("REGISTERED RULE: "+parsed.name);
            rules.put(parsed.name, parsed);
        }
    }

    public static Iterable<String> getCategories()
    {
        Set<String> categories = new HashSet<>();
        getRules().stream().map(r -> r.categories).forEach(categories::addAll);
        return categories;
    }


    public ParsedRule getRule(String name) {
        return rules.get(name);
    }

    public static Collection<ParsedRule<?>> getRules() {
        CarpetSettings.LOG.error("grabbing rules");
        List<ParsedRule<?>> r = rules.values().stream().sorted().collect(Collectors.toList());
        CarpetSettings.LOG.error("Got: "+r.size()+" rules");
        return r;
    }

    public Collection<ParsedRule<?>> findStartupOverrides()
    {
        Set<String> defaults = readSettingsFromConf().getLeft().keySet();
        return rules.values().stream().filter(r -> defaults.contains(r.name)).
                sorted().collect(Collectors.toList());
    }


    public Collection<ParsedRule<?>> getNonDefault() {
        return rules.values().stream().filter(r -> !r.isDefault()).sorted().collect(Collectors.toList());
    }

    private File getFile() {
        return server.getLevelStorage().resolveFile(server.getLevelName(), "carpet.conf");
    }

    public void disableBooleanFromCategory(String category) {
        for (ParsedRule<?> rule : rules.values()) {
            if (rule.type != boolean.class || !rule.categories.contains(category)) continue;
            ((ParsedRule<Boolean>) rule).set(server.getCommandSource(), false, "false");
        }
    }



    private void writeSettingsToConf(Map<String, String> values)
    {
        if (locked) return;
        try
        {
            FileWriter fw  = new FileWriter(getFile());
            for (String key: values.keySet())
            {
                fw.write(key+" "+values.get(key)+"\n");
            }
            fw.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            LOG.error("[CM]: failed write the carpet.conf");
        }
        ///todo is it really needed? resendCommandTree();
    }

    public void notifyPlayersCommandsChanged()
    {
        if (server.getPlayerManager() == null)
        {
            return;
        }
        for (ServerPlayerEntity entityplayermp : server.getPlayerManager().getPlayerList())
        {
            server.getCommandManager().sendCommandTree(entityplayermp);
        }
    }

    private void loadConfigurationFromConf()
    {
        for (ParsedRule<?> rule : rules.values()) rule.resetToDefault(server.getCommandSource());
        Pair<Map<String, String>,Boolean> conf = readSettingsFromConf();
        locked = false;
        if (conf.getRight())
        {
            LOG.info("[CM]: Carpet Mod is locked by the administrator");
            disableBooleanFromCategory(RuleCategory.COMMANDS);
        }
        for (String key: conf.getLeft().keySet())
        {
            if (rules.get(key).set(server.getCommandSource(), conf.getLeft().get(key)) != null)
                LOG.info("[CM]: loaded setting "+key+" as "+conf.getLeft().get(key)+" from carpet.conf");
        }
        locked = conf.getRight();
    }


    public Pair<Map<String, String>,Boolean> readSettingsFromConf()
    {
        try
        {
            BufferedReader reader = new BufferedReader(new FileReader(getFile()));
            String line = "";
            boolean confLocked = false;
            Map<String,String> result = new HashMap<String, String>();
            while ((line = reader.readLine()) != null)
            {
                line = line.replaceAll("\\r|\\n", "");
                if ("locked".equalsIgnoreCase(line))
                {
                    confLocked = true;
                }
                String[] fields = line.split("\\s+",2);
                if (fields.length > 1)
                {
                    if (!rules.containsKey(fields[0]))
                    {
                        LOG.error("[CM]: Setting " + fields[0] + " is not a valid - ignoring...");
                        continue;
                    }
                    ParsedRule rule = rules.get(fields[0]);



                    if (!(rule.options.contains(fields[1])) && rule.isStrict)
                    {
                        LOG.error("[CM]: The value of " + fields[1] + " for " + fields[0] + " is not valid - ignoring...");
                        continue;
                    }
                    result.put(fields[0],fields[1]);

                }
            }
            reader.close();
            return Pair.of(result, confLocked);
        }
        catch(FileNotFoundException e)
        {
            return Pair.of(new HashMap<>(), false);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return Pair.of(new HashMap<>(), false);
        }
    }

    // stores different defaults in the file
    public boolean setDefaultRule(ServerCommandSource source, String settingName, String stringValue)
    {
        if (locked) return false;
        if (rules.containsKey(settingName))
        {
            Pair<Map<String, String>,Boolean> conf = readSettingsFromConf();
            conf.getLeft().put(settingName, stringValue);
            writeSettingsToConf(conf.getLeft()); // this may feels weird, but if conf
            // is locked, it will never reach this point.
            rules.get(settingName).set(source,stringValue);
            return true;
        }
        return false;
    }
    // removes overrides of the default values in the file
    public boolean removeDefaultRule(ServerCommandSource source, String settingName)
    {
        if (locked) return false;
        if (rules.containsKey(settingName))
        {
            Pair<Map<String, String>,Boolean> conf = readSettingsFromConf();
            conf.getLeft().remove(settingName);
            writeSettingsToConf(conf.getLeft());
            rules.get(settingName).resetToDefault(source);
            return true;
        }
        return false;
    }

    public Collection<ParsedRule<?>> getRulesMatching(Predicate<ParsedRule<?>> predicate) {
        return rules.values().stream().filter(predicate).collect(ImmutableList.toImmutableList());
    }

    public Collection<ParsedRule<?>> getRulesMatching(String search) {
        String lcSearch = search.toLowerCase(Locale.ROOT);
        return getRulesMatching(rule -> {
            if (rule.name.toLowerCase(Locale.ROOT).contains(lcSearch)) return true;
            for (String c : rule.categories) if (c.toLowerCase(Locale.ROOT).equals(search)) return true;
            return false;
        });
    }
}
