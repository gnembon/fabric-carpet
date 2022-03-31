package carpet.settings;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.network.ServerNetworkHandler;
import carpet.utils.Messenger;
import carpet.utils.Translations;
import com.google.common.collect.Sets;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.BaseComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.logging.log4j.util.TriConsumer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static carpet.script.CarpetEventServer.Event.CARPET_RULE_CHANGES;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.SharedSuggestionProvider.suggest;

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
    private List<TriConsumer<CommandSourceStack, ParsedRule<?>, String>> observers = new ArrayList<>();
    private static List<TriConsumer<CommandSourceStack, ParsedRule<?>, String>> staticObservers = new ArrayList<>();
    static record ConfigReadResult(Map<String, String> ruleMap, boolean locked) {}

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
        rule: for (Field f : settingsClass.getDeclaredFields())
        {
            Rule rule = f.getAnnotation(Rule.class);
            if (rule == null) continue;
            ParsedRule<?> parsed = new ParsedRule<>(f, rule, this);
            for (Class<? extends Condition> condition : rule.condition()) {
                try
                {
                    Constructor<?> constr = condition.getDeclaredConstructor();
                    constr.setAccessible(true);
                    if (!((Condition) constr.newInstance()).isTrue())
                        continue rule;
                }
                catch (ReflectiveOperationException e)
                {
                    throw new RuntimeException(e);
                }
            }
            rules.put(parsed.name, parsed);
        }
    }

    /**
     * Adds a custom rule observer to changes in rules from 
     * <b>this specific</b> {@link SettingsManager} instance.
     * @see SettingsManager#addGlobalRuleObserver(TriConsumer)
     * 
     * @param observer A {@link TriConsumer} that will be called with
     *                 the used {@link CommandSourceStack}, the changed
     *                 {@link ParsedRule} and a {@link String} being the
     *                 value that the user typed.
     */
    public void addRuleObserver(TriConsumer<CommandSourceStack, ParsedRule<?>, String> observer)
    {
        observers.add(observer);
    }
    
    /**
     * Adds a custom rule observer to changes in rules from 
     * <b>any</b> registered {@link SettingsManager} instance.
     * @see SettingsManager#addRuleObserver(TriConsumer)
     * 
     * @param observer A {@link TriConsumer} that will be called with
     *                 the used {@link CommandSourceStack}, the changed
     *                 {@link ParsedRule} and a {@link String} being the
     *                 value that the user typed.
     */
    public static void addGlobalRuleObserver(TriConsumer<CommandSourceStack, ParsedRule<?>, String> observer)
    {
        staticObservers.add(observer);
    }


    void notifyRuleChanged(CommandSourceStack source, ParsedRule<?> rule, String userTypedValue)
    {
        observers.forEach(observer -> observer.accept(source, rule, userTypedValue));
        staticObservers.forEach(observer -> observer.accept(source, rule, userTypedValue));
        ServerNetworkHandler.updateRuleWithConnectedClients(rule);
        switchScarpetRuleIfNeeded(source, rule);
        if (CARPET_RULE_CHANGES.isNeeded()) CARPET_RULE_CHANGES.onCarpetRuleChanges(rule, source);
    }
    
    private void switchScarpetRuleIfNeeded(CommandSourceStack source, ParsedRule<?> rule)
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
                switchScarpetRuleIfNeeded(server.createCommandSourceStack(), rule);
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
        Set<String> defaults = readSettingsFromConf(getFile()).ruleMap().keySet();
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
        return server.getWorldPath(LevelResource.ROOT).resolve(identifier+".conf");
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
                ((ParsedRule<Boolean>) rule).set(server.createCommandSourceStack(), false, "false");
            if (rule.type == String.class && rule.options.contains("false"))
                ((ParsedRule<String>) rule).set(server.createCommandSourceStack(), "false", "false");
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
            CarpetSettings.LOG.error("[CM]: failed write "+identifier+".conf config file", e);
        }
        ///todo is it really needed? resendCommandTree();
    }

    /**
     * Notifies all players that the commands changed by resending the command tree.
     */
    public void notifyPlayersCommandsChanged()
    {
        if (server == null || server.getPlayerList() == null)
        {
            return;
        }
        server.tell(new TickTask(this.server.getTickCount(), () ->
        {
            try {
                for (ServerPlayer entityplayermp : server.getPlayerList().getPlayers()) {
                    server.getCommands().sendCommands(entityplayermp);
                }
            }
            catch (NullPointerException ignored) {}
        }));
    }

    /**
     * Returns whether the the {@link CommandSourceStack} can execute
     * a command given the required permission level, according to
     * Carpet's standard for permissions.
     * @param source The origin {@link CommandSourceStack}
     * @param commandLevel A {@link String} being the permission level (either 0-4, a 
     *                     {@link boolean} value or "ops".
     * @return Whether or not the {@link CommandSourceStack} meets the required level
     */
    public static boolean canUseCommand(CommandSourceStack source, Object commandLevel)
    {
        if (commandLevel instanceof Boolean) return (Boolean) commandLevel;
        String commandLevelString = commandLevel.toString();
        switch (commandLevelString)
        {
            case "true": return true;
            case "false": return false;
            case "ops": return source.hasPermission(2); // typical for other cheaty commands
            case "0":
            case "1":
            case "2":
            case "3":
            case "4":
                return source.hasPermission(Integer.parseInt(commandLevelString));
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
        for (ParsedRule<?> rule : rules.values()) rule.resetToDefault(server.createCommandSourceStack());
        ConfigReadResult conf = readSettingsFromConf(getFile());
        locked = false;
        if (conf.locked())
        {
            CarpetSettings.LOG.info("[CM]: "+fancyName+" features are locked by the administrator");
            disableBooleanCommands();
        }
        for (String key: conf.ruleMap().keySet())
        {
            try
            {
                if (rules.get(key).set(server.createCommandSourceStack(), conf.ruleMap().get(key)) != null)
                    CarpetSettings.LOG.info("[CM]: loaded setting " + key + " as " + conf.ruleMap().get(key) + " from " + identifier + ".conf");
            }
            catch (Exception exc)
            {
                CarpetSettings.LOG.error("[CM Error]: Failed to load setting: "+key+", "+exc);
            }
        }
        locked = conf.locked();
    }


    private ConfigReadResult readSettingsFromConf(Path path)
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
            return new ConfigReadResult(result, confLocked);
        }
        catch (NoSuchFileException e)
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
                    CarpetSettings.LOG.error("Exception when loading fallback default config: ", e2);
                }
            }
            return new ConfigReadResult(new HashMap<>(), false);
        }
        catch (IOException e)
        {
            CarpetSettings.LOG.error("Exception while loading Carpet rules from config", e);
            return new ConfigReadResult(new HashMap<>(), false);
        }
    }

    private Collection<ParsedRule<?>> getRulesMatching(CommandSourceStack source, String search) {
        String lcSearch = search.toLowerCase(Locale.ROOT);
        return rules.values().stream().filter(rule ->
        {
            if (rule.name.toLowerCase(Locale.ROOT).contains(lcSearch)) return true; // substring match, case insensitive
            for (String c : rule.categories) if (c.equals(search)) return true; // category exactly, case sensitive
            return Sets.newHashSet(Translations.translate(rule.getDescriptionText(), source).getString().toLowerCase(Locale.ROOT).split("\\W+")).contains(lcSearch); // contains full term in description, but case insensitive
        }).sorted().collect(Collectors.toUnmodifiableList());
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
        ps.println("# " + Messenger.tr("carpet.misc.rule_printer.header", fancyName).getString());
        for (Map.Entry<String, ParsedRule<?>> e : new TreeMap<>(rules).entrySet())
        {
            ParsedRule<?> rule = e.getValue();
            if (category != null && !rule.categories.contains(category))
                continue;
            ps.println("## " + rule.getNameText().getString());
            ps.println(rule.getDescriptionText().getString()+"  ");
            for (BaseComponent extra : rule.getExtrasText())
                ps.println(extra.getString() + "  ");
            ps.println("* " + Messenger.tr("carpet.misc.rule_printer.type").getString() + ": `" + rule.type.getSimpleName() + "`  ");
            ps.println("* " + Messenger.tr("carpet.misc.rule_printer.default_value").getString() + ": `" + rule.defaultAsString + "`  ");
            String optionString = rule.options.stream().map(s -> "`" + s + "`").collect(Collectors.joining(", "));
            if (!optionString.isEmpty()) ps.println("* " + Messenger.tr("carpet.misc.rule_printer." + (rule.isStrict?"required_options":"suggested_options")).getString() + ": " + optionString + "  ");
            ps.println("* " + Messenger.tr("carpet.misc.rule_printer.categories").getString() + ": " + rule.categories.stream().map(s -> "`" + s.toUpperCase(Locale.ROOT) + "`").collect(Collectors.joining(", ")) + "  ");
            boolean preamble = false;
            for (Validator<?> validator : rule.validators)
            {
                BaseComponent description = validator.descriptionText();
                if(description != null)
                {
                    if (!preamble)
                    {
                        ps.println("* " + Messenger.tr("carpet.misc.rule_printer.additional_notes").getString() + ":  ");
                        preamble = true;
                    }
                    ps.println("  * "+description.getString()+"  ");
                }
            }
            ps.println("  ");
        }
        return 1;
    }


    private ParsedRule<?> contextRule(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        String ruleName = StringArgumentType.getString(ctx, "rule");
        ParsedRule<?> rule = getRule(ruleName);
        if (rule == null)
            throw new SimpleCommandExceptionType(Messenger.c("rb", Messenger.tr("carpet.command.carpet.unknown_rule"), "rb : " + ruleName)).create();
        return rule;
    }

    static CompletableFuture<Suggestions> suggestMatchingContains(Stream<String> stream, SuggestionsBuilder suggestionsBuilder) {
        List<String> regularSuggestionList = new ArrayList<>();
        List<String> smartSuggestionList = new ArrayList<>();
        String query = suggestionsBuilder.getRemaining().toLowerCase(Locale.ROOT);
        stream.forEach((listItem) -> {
            // Regex camelCase Search
            var words = Arrays.stream(listItem.split("(?<!^)(?=[A-Z])")).map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toList());
            var prefixes = new ArrayList<String>(words.size());
            for (int i = 0; i < words.size(); i++)
                prefixes.add(String.join("",words.subList(i, words.size())));
            if (prefixes.stream().anyMatch(s -> s.startsWith(query))) {
                smartSuggestionList.add(listItem);
            }
            // Regular prefix matching, reference: CommandSource.suggestMatching
            if (SharedSuggestionProvider.matchesSubStr(query, listItem.toLowerCase(Locale.ROOT))) {
                regularSuggestionList.add(listItem);
            }
        });
        var filteredSuggestionList = regularSuggestionList.isEmpty() ? smartSuggestionList : regularSuggestionList;
        Objects.requireNonNull(suggestionsBuilder);
        filteredSuggestionList.forEach(suggestionsBuilder::suggest);
        return suggestionsBuilder.buildFuture();
    }

    private BaseComponent categoryName(String category)
    {
        String key = identifier + ".category." + category;
        // if the category does not have translation, use itself as displayed name
        if (Translations.key2Translation(Translations.DEFAULT_LANGUAGE, key).isEmpty())
        {
            return Messenger.s(category);
        }
        return Messenger.tr(key);
    }

    /**
     * Registers the the settings command for this {@link SettingsManager}.<br>
     * It is handled automatically by Carpet.
     * @param dispatcher The current {@link CommandDispatcher}
     */
    public void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        if (dispatcher.getRoot().getChildren().stream().anyMatch(node -> node.getName().equalsIgnoreCase(identifier)))
        {
            CarpetSettings.LOG.error("Failed to add settings command for " + identifier + ". It is masking previous command.");
            return;
        }

        LiteralArgumentBuilder<CommandSourceStack> literalargumentbuilder = literal(identifier).requires((player) ->
                canUseCommand(player, CarpetSettings.carpetCommandPermissionLevel) && !locked);

        literalargumentbuilder.executes((context)-> listAllSettings(context.getSource())).
                then(literal("list").
                        executes( (c) -> listSettings(c.getSource(), Messenger.tr("carpet.command.carpet.all_settings", fancyName),
                                getRules())).
                        then(literal("defaults").
                                executes( (c)-> listSettings(c.getSource(),
                                        Messenger.tr("carpet.command.carpet.current_startup_settings", fancyName, (identifier+".conf")),
                                        findStartupOverrides()))).
                        then(argument("tag",StringArgumentType.word()).
                                suggests( (c, b)->suggest(getCategories(), b)).
                                executes( (c) -> listSettings(c.getSource(),
                                        Messenger.tr("carpet.command.carpet.settings_matching", fancyName, categoryName(StringArgumentType.getString(c, "tag"))),
                                        getRulesMatching(c.getSource(), StringArgumentType.getString(c, "tag")))))).
                then(literal("removeDefault").
                        requires(s -> !locked).
                        then(argument("rule", StringArgumentType.word()).
                                suggests( (c, b) -> suggestMatchingContains(getRules().stream().map(r -> r.name), b)).
                                executes((c) -> removeDefault(c.getSource(), contextRule(c))))).
                then(literal("setDefault").
                        requires(s -> !locked).
                        then(argument("rule", StringArgumentType.word()).
                                suggests( (c, b) -> suggestMatchingContains(getRules().stream().map(r -> r.name), b)).
                                then(argument("value", StringArgumentType.greedyString()).
                                        suggests((c, b)-> suggest(contextRule(c).options, b)).
                                        executes((c) -> setDefault(c.getSource(), contextRule(c), StringArgumentType.getString(c, "value")))))).
                then(argument("rule", StringArgumentType.word()).
                        suggests( (c, b) -> suggestMatchingContains(getRules().stream().map(r -> r.name), b)).
                        requires(s -> !locked ).
                        executes( (c) -> displayRuleMenu(c.getSource(), contextRule(c))).
                        then(argument("value", StringArgumentType.greedyString()).
                                suggests((c, b)-> suggest(contextRule(c).options,b)).
                                executes((c) -> setRule(c.getSource(), contextRule(c), StringArgumentType.getString(c, "value")))));

        dispatcher.register(literalargumentbuilder);
    }

    private int displayRuleMenu(CommandSourceStack source, ParsedRule<?> rule)
    {
        Messenger.m(source, "");
        Messenger.m(source, "wb", rule.getNameText(source), "!/%s %s".formatted(identifier, rule.name), "^g", Messenger.tr("carpet.command.carpet.refresh"));
        Messenger.m(source, rule.getDescriptionText());

        rule.getExtrasText(source).forEach(extra -> Messenger.m(source, "g", extra));

        List<Object> tags = new ArrayList<>();
        tags.add(Messenger.c(Messenger.tr("carpet.command.carpet.tags"), "w : "));
        for (String category : rule.categories)
        {
            BaseComponent translated = categoryName(category);
            tags.add(Messenger.c("c [", "c", translated, "c ]"));
            tags.add("^g"); tags.add(Messenger.tr("carpet.command.carpet.list_all_settings", translated));
            tags.add("!/%s list %s".formatted(identifier, category));
            tags.add(Messenger.c("w , "));
        }
        tags.remove(tags.size()-1);
        Messenger.m(source, tags.toArray(new Object[0]));

        Messenger.m(source,
                Messenger.tr("carpet.command.carpet.current_value"),
                "w : ",
                rule.getBoolValue() ? "lb" : "nb",
                Messenger.c(" " + rule.getAsString() + " (", Messenger.tr(rule.isDefault() ? "carpet.command.carpet.default_value" : "carpet.command.carpet.modified_value"), " )")
        );
        List<BaseComponent> options = new ArrayList<>();
        options.add(Messenger.c(Messenger.tr("carpet.command.carpet.options"), "w : ", "y [ "));
        for (String o: rule.options)
        {
            options.add(makeSetRuleButton(rule, o, false));
            options.add(Messenger.c("w  "));
        }
        if (!rule.options.isEmpty()) options.remove(options.size() - 1);
        options.add(Messenger.c("y  ]"));
        Messenger.m(source, options.toArray(new Object[0]));

        return 1;
    }

    private int setRule(CommandSourceStack source, ParsedRule<?> rule, String newValue)
    {
        if (rule.set(source, newValue) != null)
            Messenger.m(source,
                    "w "+rule+", ", Messenger.c("c [", "c", Messenger.tr("carpet.command.carpet.change_permanently"), "c ]"),
                    "^", Messenger.tr("carpet.command.carpet.click_to_save_the_setting", identifier+".conf"),
                    "?/%s setDefault %s %s".formatted(identifier, rule.name, rule.getAsString())
            );
        return 1;
    }

    // stores different defaults in the file
    private int setDefault(CommandSourceStack source, ParsedRule<?> rule, String stringValue)
    {
        if (locked) return 0;
        if (!rules.containsKey(rule.name)) return 0;
        ConfigReadResult conf = readSettingsFromConf(getFile());
        conf.ruleMap().put(rule.name, stringValue);
        writeSettingsToConf(conf.ruleMap()); // this may feels weird, but if conf
        // is locked, it will never reach this point.
        rule.set(source,stringValue);
        Messenger.m(source ,"gi", Messenger.tr("carpet.command.carpet.set_default", rule.getNameText(source), stringValue));
        return 1;
    }
    // removes overrides of the default values in the file
    private int removeDefault(CommandSourceStack source, ParsedRule<?> rule)
    {
        if (locked) return 0;
        if (!rules.containsKey(rule.name)) return 0;
        ConfigReadResult conf = readSettingsFromConf(getFile());
        conf.ruleMap().remove(rule.name);
        writeSettingsToConf(conf.ruleMap());
        rules.get(rule.name).resetToDefault(source);
        Messenger.m(source ,"gi", Messenger.tr("carpet.command.carpet.remove_default", rule.getNameText(source)));
        return 1;
    }

    private BaseComponent displayInteractiveSetting(CommandSourceStack source, ParsedRule<?> rule)
    {
        List<Object> args = new ArrayList<>();
        args.add(Messenger.c("w - ", rule.getNameText(source), "w  "));
        args.add("!/%s %s".formatted(identifier, rule.name));
        args.add("^y"); args.add(rule.getDescriptionText());
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

    private BaseComponent makeSetRuleButton(ParsedRule<?> rule, String option, boolean brackets)
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
            return Messenger.c(baseText, "^g", Messenger.c("g " + fancyName + " ", Messenger.tr("carpet.command.carpet.settings_are_locked")));
        if (option.equalsIgnoreCase(rule.getAsString()))
            return Messenger.c(baseText);
        return Messenger.c(baseText,
                "^g", Messenger.c("g", Messenger.tr("carpet.command.carpet.switch_to"), "g " + option, option.equalsIgnoreCase(rule.defaultAsString) ? Messenger.c("g  (", "g", Messenger.tr("carpet.command.carpet.default"), "g )"): Messenger.s("")),
                "?/%s %s %s".formatted(identifier, rule.name, option)
        );
    }

    private int listSettings(CommandSourceStack source, BaseComponent title, Collection<ParsedRule<?>> settings_list)
    {
        Messenger.m(source, "wb", title, "wb :");
        settings_list.forEach(e -> Messenger.m(source,displayInteractiveSetting(source, e)));
        return settings_list.size();
    }
    private int listAllSettings(CommandSourceStack source)
    {
        int count = listSettings(source, Messenger.tr("carpet.command.carpet.current_settings", fancyName), getNonDefault());

        if (version != null)
            Messenger.m(source, "g " + fancyName + " ", "g", Messenger.tr("carpet.command.carpet.version"), "g : "+ version);

        List<Object> tags = new ArrayList<>();
        tags.add(Messenger.tr("carpet.command.carpet.browse_categories"));
        tags.add("w :\n");
        for (String category : getCategories())
        {
            BaseComponent translated = categoryName(category);
            BaseComponent translatedPlus = !Translations.translate(translated, source).getString().equals(category) ? Messenger.c(translated, "  (" + category + ")") : translated;
            tags.add(Messenger.c("c [", "c", translated, "c ]"));
            tags.add("^g"); tags.add(Messenger.tr("carpet.command.carpet.list_all_settings", translatedPlus));
            tags.add("!/%s list %s".formatted(identifier, category));
            tags.add("w  ");
        }
        tags.remove(tags.size() - 1);
        Messenger.m(source, tags.toArray(new Object[0]));

        return count;
    }

    public void inspectClientsideCommand(CommandSourceStack source, String string)
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
