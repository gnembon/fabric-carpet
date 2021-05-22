package carpet.settings;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.network.ServerNetworkHandler;
import carpet.utils.Translations;
import carpet.utils.Messenger;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTask;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.BaseText;
import net.minecraft.util.WorldSavePath;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.util.TriConsumer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static carpet.utils.Translations.tr;
import static carpet.script.CarpetEventServer.Event.CARPET_RULE_CHANGES;
import static net.minecraft.command.CommandSource.suggestMatching;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Manages and parses Carpet rules with their own command.
 * @see #SettingsManager(String, String, String)
 * 
 */
public class SettingsManager
{
    private Map<String, ParsedRule<?>> rules = new HashMap<>();
    /**
     * Whether or not this {@link SettingsManager} is locked. That is specified
     * by writing "locked" at the beginning of the settings file. May not
     * correctly react to changes.
     */
    public boolean locked;
    private final String version;
    private final String identifier;
    private final String fancyName;
    private MinecraftServer server;
    private List<TriConsumer<ServerCommandSource, ParsedRule<?>, String>> observers = new ArrayList<>();
    private static List<TriConsumer<ServerCommandSource, ParsedRule<?>, String>> staticObservers = new ArrayList<>();

    /**
     * Creates a new {@link SettingsManager} without a fancy name.
     * @see #SettingsManager(String, String, String)
     * 
     * @param version A {@link String} with the mod's version
     * @param identifier A {@link String} with the mod's id, will be the command name
     */
    public SettingsManager(String version, String identifier)
    {
        this.version = version;
        this.identifier = identifier;
        this.fancyName = identifier;
    }

    /**
     * Creates a new {@link SettingsManager} with a fancy name
     * @see #SettingsManager(String, String)
     * 
     * @param version A {@link String} with the mod's version
     * @param identifier A {@link String} with the mod's id, will be the command name
     * @param fancyName A {@link String} being the mod's fancy name.
     */
    public SettingsManager(String version, String identifier, String fancyName)
    {
        this.version = version;
        this.identifier = identifier;
        this.fancyName = fancyName;
    }

    /**
     * @return A {@link String} being this {@link SettingsManager}'s 
     *         identifier, which is also the command name
     */
    public String getIdentifier() {
        return identifier;
    }
    
    /**
     * Attaches a {@link MinecraftServer} to this {@link SettingsManager}.<br>
     * This is handled automatically by Carpet
     * @param server The {@link MinecraftServer} instance to be attached
     */
    public void attachServer(MinecraftServer server)
    {
        this.server = server;
        loadConfigurationFromConf();
    }

    /**
     * Detaches the {@link MinecraftServer} of this {@link SettingsManager} and
     * resets its {@link ParsedRule}s to their default values.<br>
     * This is handled automatically by Carpet
     */
    public void detachServer()
    {
        for (ParsedRule<?> rule : rules.values()) rule.resetToDefault(null);
        server = null;
    }

    /**
     * Adds all annotated fields with the {@link Rule} annotation 
     * to this {@link SettingsManager} in order to handle them. 
     * @param settingsClass The class that will be analyzed
     */
    public void parseSettingsClass(Class settingsClass)
    {
        for (Field f : settingsClass.getDeclaredFields())
        {
            Rule rule = f.getAnnotation(Rule.class);
            if (rule == null) continue;
            ParsedRule parsed = new ParsedRule(f, rule, this);
            rules.put(parsed.name, parsed);
        }
    }

    /**
     * Adds a custom rule observer to changes in rules from 
     * <b>this specific</b> {@link SettingsManager} instance.
     * @see SettingsManager#addGlobalRuleObserver(TriConsumer)
     * 
     * @param observer A {@link TriConsumer} that will be called with
     *                 the used {@link ServerCommandSource}, the changed
     *                 {@link ParsedRule} and a {@link String} being the
     *                 value that the user typed.
     */
    public void addRuleObserver(TriConsumer<ServerCommandSource, ParsedRule<?>, String> observer)
    {
        observers.add(observer);
    }
    
    /**
     * Adds a custom rule observer to changes in rules from 
     * <b>any</b> registered {@link SettingsManager} instance.
     * @see SettingsManager#addRuleObserver(TriConsumer)
     * 
     * @param observer A {@link TriConsumer} that will be called with
     *                 the used {@link ServerCommandSource}, the changed
     *                 {@link ParsedRule} and a {@link String} being the
     *                 value that the user typed.
     */
    public static void addGlobalRuleObserver(TriConsumer<ServerCommandSource, ParsedRule<?>, String> observer)
    {
        staticObservers.add(observer);
    }


    void notifyRuleChanged(ServerCommandSource source, ParsedRule<?> rule, String userTypedValue)
    {
        observers.forEach(observer -> observer.accept(source, rule, userTypedValue));
        staticObservers.forEach(observer -> observer.accept(source, rule, userTypedValue));
        ServerNetworkHandler.updateRuleWithConnectedClients(rule);
        switchScarpetRuleIfNeeded(source, rule);
        if (CARPET_RULE_CHANGES.isNeeded()) CARPET_RULE_CHANGES.onCarpetRuleChanges(rule, source);
    }
    
    private void switchScarpetRuleIfNeeded(ServerCommandSource source, ParsedRule<?> rule)
    {
        if (!rule.scarpetApp.isEmpty())
        {
            if (rule.getBoolValue() || (rule.type == String.class && !rule.get().equals("false")))
            {
                CarpetServer.scriptServer.addScriptHost(source, rule.scarpetApp, s -> canUseCommand(s, rule.get()), false, false, true, null);
            } else {
                CarpetServer.scriptServer.removeScriptHost(source, rule.scarpetApp, false, true);
            }
        }
    }
    
    /**
     * Initializes Scarpet rules in this {@link SettingsManager}, if any.<br>
     * This is handled automatically by Carpet.
     */
    public void initializeScarpetRules() {
        for (ParsedRule<?> rule : rules.values())
        {
            if (!rule.scarpetApp.isEmpty()) {
                switchScarpetRuleIfNeeded(server.getCommandSource(), rule);
            }
        }
    }

    /**
     * @return An {@link Iterable} list of all categories
     *         from rules in this {@link SettingsManager}
     */
    public Iterable<String> getCategories()
    {
        Set<String> categories = new HashSet<>();
        getRules().stream().map(r -> r.categories).forEach(categories::addAll);
        return categories;
    }


    /**
     * Gets a registered rule in this {@link SettingsManager} by its name.
     * @param name The rule name
     * @return The {@link ParsedRule} with that name
     */
    public ParsedRule<?> getRule(String name)
    {
        return rules.get(name);
    }

    /**
     * @return A {@link Collection} of the registered rules in this
     *         {@link SettingsManager}.
     */
    public Collection<ParsedRule<?>> getRules()
    {
        return rules.values().stream().sorted().collect(Collectors.toList());
    }

    /**
     * @return A {@link Collection} of {@link ParsedRule}s that have defaults
     *         for this world different than the rule's default value.
     */
    public Collection<ParsedRule<?>> findStartupOverrides()
    {
        Set<String> defaults = readSettingsFromConf(getFile()).getLeft().keySet();
        return rules.values().stream().filter(r -> defaults.contains(r.name)).
                sorted().collect(Collectors.toList());
    }

    /**
     * @return A collection of {@link ParsedRule} that are not in 
     *         their default value
     */
    public Collection<ParsedRule<?>> getNonDefault()
    {
        return rules.values().stream().filter(r -> !r.isDefault()).sorted().collect(Collectors.toList());
    }

    private Path getFile()
    {
        return server.getSavePath(WorldSavePath.ROOT).resolve(identifier+".conf");
    }

    /**
     * Disables all {@link ParsedRule} with the {@link RuleCategory#COMMAND} category,
     * called when the {@link SettingsManager} is {@link #locked}.
     */
    public void disableBooleanCommands()
    {
        for (ParsedRule<?> rule : rules.values())
        {
            if (!rule.categories.contains(RuleCategory.COMMAND))
                continue;
            if (rule.type == boolean.class)
                ((ParsedRule<Boolean>) rule).set(server.getCommandSource(), false, "false");
            if (rule.type == String.class && rule.options.contains("false"))
                ((ParsedRule<String>) rule).set(server.getCommandSource(), "false", "false");
        }
    }


    private void writeSettingsToConf(Map<String, String> values)
    {
        if (locked)
            return;
        try (BufferedWriter fw = Files.newBufferedWriter(getFile()))
        {
            for (String key: values.keySet())
            {
                fw.write(key+" "+values.get(key));
                fw.newLine();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
            CarpetSettings.LOG.error("[CM]: failed write "+identifier+".conf config file");
        }
        ///todo is it really needed? resendCommandTree();
    }

    /**
     * Notifies all players that the commands changed by resending the command tree.
     */
    public void notifyPlayersCommandsChanged()
    {
        if (server == null || server.getPlayerManager() == null)
        {
            return;
        }
        server.send(new ServerTask(this.server.getTicks(), () ->
        {
            try {
                for (ServerPlayerEntity entityplayermp : server.getPlayerManager().getPlayerList()) {
                    server.getCommandManager().sendCommandTree(entityplayermp);
                }
            }
            catch (NullPointerException ignored) {}
        }));
    }

    /**
     * Returns whether the the {@link ServerCommandSource} can execute
     * a command given the required permission level, according to
     * Carpet's standard for permissions.
     * @param source The origin {@link ServerCommandSource}
     * @param commandLevel A {@link String} being the permission level (either 0-4, a 
     *                     {@link boolean} value or "ops".
     * @return Whether or not the {@link ServerCommandSource} meets the required level
     */
    public static boolean canUseCommand(ServerCommandSource source, Object commandLevel)
    {
        if (commandLevel instanceof Boolean) return (Boolean) commandLevel;
        String commandLevelString = commandLevel.toString();
        switch (commandLevelString)
        {
            case "true": return true;
            case "false": return false;
            case "ops": return source.hasPermissionLevel(2); // typical for other cheaty commands
            case "0":
            case "1":
            case "2":
            case "3":
            case "4":
                return source.hasPermissionLevel(Integer.parseInt(commandLevelString));
        }
        return false;
    }

    /**
     * @param commandLevel A {@link String} being a permission level according to 
     *                     Carpet's standard for permissions (either 0-4, a {@link boolean}, 
     *                     or "ops".
     * @return An {@link int} with the translated Vanilla permission level
     */
    public static int getCommandLevel(String commandLevel)
    {
        switch (commandLevel)
        {
            case "true": return 2;
            case "false": return 0;
            case "ops": return 2; // typical for other cheaty commands
            case "0":
            case "1":
            case "2":
            case "3":
            case "4":
                return Integer.parseInt(commandLevel);
        }
        return 0;
    }


    private void loadConfigurationFromConf()
    {
        for (ParsedRule<?> rule : rules.values()) rule.resetToDefault(server.getCommandSource());
        Pair<Map<String, String>, Boolean> conf = readSettingsFromConf(getFile());
        locked = false;
        if (conf.getRight())
        {
            CarpetSettings.LOG.info("[CM]: "+fancyName+" features are locked by the administrator");
            disableBooleanCommands();
        }
        for (String key: conf.getLeft().keySet())
        {
            try
            {
                if (rules.get(key).set(server.getCommandSource(), conf.getLeft().get(key)) != null)
                    CarpetSettings.LOG.info("[CM]: loaded setting " + key + " as " + conf.getLeft().get(key) + " from " + identifier + ".conf");
            }
            catch (Exception exc)
            {
                CarpetSettings.LOG.error("[CM Error]: Failed to load setting: "+key+", "+exc);
            }
        }
        locked = conf.getRight();
    }


    private Pair<Map<String, String>, Boolean> readSettingsFromConf(Path path)
    {
        try (BufferedReader reader = Files.newBufferedReader(path))
        {
            String line = "";
            boolean confLocked = false;
            Map<String,String> result = new HashMap<String, String>();
            while ((line = reader.readLine()) != null)
            {
                line = line.replaceAll("[\\r\\n]", "");
                if ("locked".equalsIgnoreCase(line))
                {
                    confLocked = true;
                }
                String[] fields = line.split("\\s+",2);
                if (fields.length > 1)
                {
                    if (!rules.containsKey(fields[0]))
                    {
                        CarpetSettings.LOG.error("[CM]: "+fancyName+" Setting " + fields[0] + " is not a valid - ignoring...");
                        continue;
                    }
                    ParsedRule rule = rules.get(fields[0]);

                    if (!(rule.options.contains(fields[1])) && rule.isStrict)
                    {
                        CarpetSettings.LOG.error("[CM]: The value of " + fields[1] + " for " + fields[0] + "("+fancyName+") is not valid - ignoring...");
                        continue;
                    }
                    result.put(fields[0],fields[1]);

                }
            }
            return Pair.of(result, confLocked);
        }
        catch(NoSuchFileException e)
        {
            if (path.equals(getFile()) && FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT)
            {
                Path defaultsPath = FabricLoader.getInstance().getConfigDir().resolve("carpet/default_"+identifier+".conf");
                try {
                    if (Files.notExists(defaultsPath))
                    {
                        Files.createDirectories(defaultsPath.getParent());
                        Files.createFile(defaultsPath);
                    }
                    return readSettingsFromConf(defaultsPath);
                } catch (IOException e2) {
                    CarpetSettings.LOG.error("Exception when loading fallback default config: ", e);
                }
            }
            return Pair.of(new HashMap<>(), false);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return Pair.of(new HashMap<>(), false);
        }
    }

    private Collection<ParsedRule<?>> getRulesMatching(String search) {
        String lcSearch = search.toLowerCase(Locale.ROOT);
        return rules.values().stream().filter(rule ->
        {
            if (rule.name.toLowerCase(Locale.ROOT).contains(lcSearch)) return true; // substring match, case insensitive
            for (String c : rule.categories) if (c.equals(search)) return true; // category exactly, case sensitive
            return Sets.newHashSet(rule.description.toLowerCase(Locale.ROOT).split("\\W+")).contains(lcSearch); // contains full term in description, but case insensitive
        }).sorted().collect(ImmutableList.toImmutableList());
    }

    /**
     * A method to pretty print in markdown (useful for Github wiki/readme) the current
     * registered rules for a category to the log. Contains the name, description,
     * categories, type, defaults, wether or not they are strict, their suggested
     * values, and the descriptions of their validators.
     * @param category A {@link String} being the category to print, {@link null} to print
     *                 all registered rules.
     * @return {@link int} 1 if things didn't fail, and all the info to System.out
     */
    public int printAllRulesToLog(String category)
    {
        PrintStream ps = System.out;
        ps.println("# "+fancyName+" Settings");
        for (Map.Entry<String, ParsedRule<?>> e : new TreeMap<>(rules).entrySet())
        {
            ParsedRule<?> rule = e.getValue();
            if (category != null && !rule.categories.contains(category))
                continue;
            ps.println("## " + rule.name);
            ps.println(rule.description+"  ");
            for (String extra : rule.extraInfo)
                ps.println(extra + "  ");
            ps.println("* Type: `" + rule.type.getSimpleName() + "`  ");
            ps.println("* Default value: `" + rule.defaultAsString + "`  ");
            String optionString = rule.options.stream().map(s -> "`" + s + "`").collect(Collectors.joining(", "));
            ps.println((rule.isStrict?"* Required":"* Suggested")+" options: " + optionString + "  ");
            ps.println("* Categories: " + rule.categories.stream().map(s -> "`" + s.toUpperCase(Locale.ROOT) + "`").collect(Collectors.joining(", ")) + "  ");
            boolean preamble = false;
            for (Validator<?> validator : rule.validators)
            {
                if(validator.description() != null)
                {
                    if (!preamble)
                    {
                        ps.println("* Additional notes:  ");
                        preamble = true;
                    }
                    ps.println("  * "+validator.description()+"  ");
                }
            }
            ps.println("  ");
        }
        return 1;
    }


    private ParsedRule<?> contextRule(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException
    {
        String ruleName = StringArgumentType.getString(ctx, "rule");
        ParsedRule<?> rule = getRule(ruleName);
        if (rule == null)
            throw new SimpleCommandExceptionType(Messenger.c("rb "+ tr("ui.unknown_rule","Unknown rule")+": "+ruleName)).create();
        return rule;
    }

    /**
     * Registers the the settings command for this {@link SettingsManager}.<br>
     * It is handled automatically by Carpet.
     * @param dispatcher The current {@link CommandDispatcher}
     */
    public void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher)
    {
        if (dispatcher.getRoot().getChildren().stream().anyMatch(node -> node.getName().equalsIgnoreCase(identifier)))
        {
            CarpetSettings.LOG.error("Failed to add settings command for " + identifier + ". It is masking previous command.");
            return;
        }

        LiteralArgumentBuilder<ServerCommandSource> literalargumentbuilder = literal(identifier).requires((player) ->
                player.hasPermissionLevel(2) && !locked);

        literalargumentbuilder.executes((context)-> listAllSettings(context.getSource())).
                then(literal("list").
                        executes( (c) -> listSettings(c.getSource(), String.format(tr("ui.all_%(mod)s_settings","All %s Settings"), fancyName),
                                getRules())).
                        then(literal("defaults").
                                executes( (c)-> listSettings(c.getSource(),
                                        String.format(tr("ui.current_%(mod)s_startup_settings_from_%(conf)s","Current %s Startup Settings from %s"), fancyName, (identifier+".conf")),
                                        findStartupOverrides()))).
                        then(argument("tag",StringArgumentType.word()).
                                suggests( (c, b)->suggestMatching(getCategories(), b)).
                                executes( (c) -> listSettings(c.getSource(),
                                        String.format(tr("ui.%(mod)s_settings_matching_'%(query)s'","%s Settings matching \"%s\""), fancyName, tr("category." + StringArgumentType.getString(c, "tag"),StringArgumentType.getString(c, "tag"))),
                                        getRulesMatching(StringArgumentType.getString(c, "tag")))))).
                then(literal("removeDefault").
                        requires(s -> !locked).
                        then(argument("rule", StringArgumentType.word()).
                                suggests( (c, b) -> suggestMatching(getRules().stream().map(r -> r.name), b)).
                                executes((c) -> removeDefault(c.getSource(), contextRule(c))))).
                then(literal("setDefault").
                        requires(s -> !locked).
                        then(argument("rule", StringArgumentType.word()).
                                suggests( (c, b) -> suggestMatching(getRules().stream().map(r -> r.name), b)).
                                then(argument("value", StringArgumentType.greedyString()).
                                        suggests((c, b)-> suggestMatching(contextRule(c).options, b)).
                                        executes((c) -> setDefault(c.getSource(), contextRule(c), StringArgumentType.getString(c, "value")))))).
                then(argument("rule", StringArgumentType.word()).
                        suggests( (c, b) -> suggestMatching(getRules().stream().map(r -> r.name), b)).
                        requires(s -> !locked ).
                        executes( (c) -> displayRuleMenu(c.getSource(), contextRule(c))).
                        then(argument("value", StringArgumentType.greedyString()).
                                suggests((c, b)-> suggestMatching(contextRule(c).options,b)).
                                executes((c) -> setRule(c.getSource(), contextRule(c), StringArgumentType.getString(c, "value")))));

        dispatcher.register(literalargumentbuilder);
    }

    private int displayRuleMenu(ServerCommandSource source, ParsedRule<?> rule)
    {
        PlayerEntity player;
        String displayName = rule.translatedName();

        Messenger.m(source, "");
        Messenger.m(source, "wb "+ displayName ,"!/"+identifier+" "+rule.name,"^g refresh");
        Messenger.m(source, "w "+ rule.translatedDescription());

        rule.translatedExtras().forEach(s -> Messenger.m(source, "g  "+s));

        List<BaseText> tags = new ArrayList<>();
        tags.add(Messenger.c("w "+ tr("ui.tags", "Tags")+": "));
        for (String t: rule.categories)
        {
            String translated = tr("category." + t, t);
            tags.add(Messenger.c("c ["+ translated +"]", "^g "+ String.format(tr("list_all_%s_settings","list all %s settings"), translated),"!/"+identifier+" list "+t));
            tags.add(Messenger.c("w , "));
        }
        tags.remove(tags.size()-1);
        Messenger.m(source, tags.toArray(new Object[0]));

        Messenger.m(source, "w "+ tr("ui.current_value", "Current value")+": ",String.format("%s %s (%s value)",rule.getBoolValue()?"lb":"nb", rule.getAsString(),rule.isDefault()?"default":"modified"));
        List<BaseText> options = new ArrayList<>();
        options.add(Messenger.c("w Options: ", "y [ "));
        for (String o: rule.options)
        {
            options.add(makeSetRuleButton(rule, o, false));
            options.add(Messenger.c("w  "));
        }
        options.remove(options.size()-1);
        options.add(Messenger.c("y  ]"));
        Messenger.m(source, options.toArray(new Object[0]));

        return 1;
    }

    private int setRule(ServerCommandSource source, ParsedRule<?> rule, String newValue)
    {
        if (rule.set(source, newValue) != null)
            Messenger.m(source, "w "+rule.toString()+", ", "c ["+ tr("ui.change_permanently","change permanently")+"?]",
                    "^w "+String.format(tr("ui.click_to_keep_the_settings_in_%(conf)s_to_save_across_restarts","Click to keep the settings in %s to save across restarts"), identifier+".conf"),
                    "?/"+identifier+" setDefault "+rule.name+" "+rule.getAsString());
        return 1;
    }

    // stores different defaults in the file
    private int setDefault(ServerCommandSource source, ParsedRule<?> rule, String stringValue)
    {
        if (locked) return 0;
        if (!rules.containsKey(rule.name)) return 0;
        Pair<Map<String, String>,Boolean> conf = readSettingsFromConf(getFile());
        conf.getLeft().put(rule.name, stringValue);
        writeSettingsToConf(conf.getLeft()); // this may feels weird, but if conf
        // is locked, it will never reach this point.
        rule.set(source,stringValue);
        Messenger.m(source ,"gi "+String.format(tr("ui.rule_%(rule)s_will_now_default_to_%(value)s","Rule %s will now default to %s"), rule.translatedName(), stringValue));
        return 1;
    }
    // removes overrides of the default values in the file
    private int removeDefault(ServerCommandSource source, ParsedRule<?> rule)
    {
        if (locked) return 0;
        if (!rules.containsKey(rule.name)) return 0;
        Pair<Map<String, String>,Boolean> conf = readSettingsFromConf(getFile());
        conf.getLeft().remove(rule.name);
        writeSettingsToConf(conf.getLeft());
        rules.get(rule.name).resetToDefault(source);
        Messenger.m(source ,"gi "+String.format(tr("ui.rule_%(rule)s_not_set_restart","Rule %s will now not be set on restart"), rule.translatedName()));
        return 1;
    }

    private BaseText displayInteractiveSetting(ParsedRule<?> rule)
    {
        String displayName = rule.translatedName();
        List<Object> args = new ArrayList<>();
        args.add("w - "+ displayName +" ");
        args.add("!/"+identifier+" "+rule.name);
        args.add("^y "+rule.translatedDescription());
        for (String option: rule.options)
        {
            args.add(makeSetRuleButton(rule, option, true));
            args.add("w  ");
        }
        if (!rule.options.contains(rule.getAsString()))
        {
            args.add(makeSetRuleButton(rule, rule.getAsString(), true));
            args.add("w  ");
        }
        args.remove(args.size()-1);
        return Messenger.c(args.toArray(new Object[0]));
    }

    private BaseText makeSetRuleButton(ParsedRule<?> rule, String option, boolean brackets)
    {
        String style = rule.isDefault()?"g":(option.equalsIgnoreCase(rule.defaultAsString)?"e":"y");
        if (option.equalsIgnoreCase(rule.getAsString()))
        {
            style = style + "u";
            if (option.equalsIgnoreCase(rule.defaultAsString))
                style = style + "b";
        }
        String baseText = style + (brackets ? " [" : " ") + option + (brackets ? "]" : "");
        if (locked)
            return Messenger.c(baseText, "^g "+fancyName+" " + tr("ui.settings_are_locked","settings are locked"));
        if (option.equalsIgnoreCase(rule.getAsString()))
            return Messenger.c(baseText);
        return Messenger.c(baseText, "^g "+ tr("ui.switch_to","Switch to") +" " + option+(option.equalsIgnoreCase(rule.defaultAsString)?" (default)":""), "?/"+identifier+" " + rule.name + " " + option);
    }

    private int listSettings(ServerCommandSource source, String title, Collection<ParsedRule<?>> settings_list)
    {

        Messenger.m(source,String.format("wb %s:",title));
        settings_list.forEach(e -> Messenger.m(source,displayInteractiveSetting(e)));
        return settings_list.size();
    }
    private int listAllSettings(ServerCommandSource source)
    {
        int count = listSettings(source, String.format(tr("ui.current_%(mod)s_settings","Current %s Settings"), fancyName), getNonDefault());

        if (version != null)
            Messenger.m(source, "g "+fancyName+" "+ tr("ui.version",  "version") + ": "+ version);

        List<Object> tags = new ArrayList<>();
        tags.add("w " + tr("ui.browse_categories", "Browse Categories")  + ":\n");
        for (String t : getCategories())
        {
            String catKey = "category." + t;
            String translated = tr(catKey, t);
            String translatedPlus = Translations.hasTranslation(catKey) ? String.format("%s (%s)",tr(catKey, t), t) : t;
            tags.add("c [" + translated +"]");
            tags.add("^g " + String.format(tr("ui.list_all_%(cat)s_settings","list all %s settings"), translatedPlus));
            tags.add("!/"+identifier+" list " + t);
            tags.add("w  ");
        }
        tags.remove(tags.size() - 1);
        Messenger.m(source, tags.toArray(new Object[0]));

        return count;
    }

    public void inspectClientsideCommand(ServerCommandSource source, String string)
    {
        if (string.startsWith("/"+identifier+" "))
        {
            String[] res = string.split("\\s+", 3);
            if (res.length == 3)
            {
                String setting = res[1];
                String strOption = res[2];
                if (rules.containsKey(setting) && rules.get(setting).isClient)
                {
                    rules.get(setting).set(source, strOption);
                }
            }
        }
    }
}
