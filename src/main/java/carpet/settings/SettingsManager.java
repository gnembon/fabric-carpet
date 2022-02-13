package carpet.settings;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.network.ServerNetworkHandler;
import carpet.utils.Translations;
import carpet.utils.Messenger;
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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.network.chat.BaseComponent;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static carpet.utils.Translations.tr;
import static carpet.script.CarpetEventServer.Event.CARPET_RULE_CHANGES;
import static net.minecraft.commands.SharedSuggestionProvider.suggest;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * Manages and parses Carpet rules with their own command.
 * @see #SettingsManager(String, String, String)
 * 
 */
public class SettingsManager
{
    private final Map<String, CarpetRule<?>> rules = new HashMap<>();
    /**
     * @deprecated Use {@link #locked()} instead
     */
    @Deprecated(forRemoval = true) //to private (or protected?)
    public boolean locked;
    private final String version;
    private final String identifier;
    private final String fancyName;
    private MinecraftServer server;
    private final List<RuleObserver> observers = new ArrayList<>();
    private static final List<RuleObserver> staticObservers = new ArrayList<>();
    static record ConfigFileData(Map<String, String> ruleMap, boolean locked, List<String> comments) {}
    
    /**
     * <p>Defines a class that can be notified about a {@link CarpetRule} changing.</p>
     * 
     * @see #ruleChanged(CommandSourceStack, CarpetRule)
     * @see SettingsManager#addRuleObserver(RuleObserver)
     * @see SettingsManager#addGlobalRuleObserver(RuleObserver)
     */
    @FunctionalInterface
    public static interface RuleObserver {
        /**
         * @param source The {@link CommandSourceStack} that likely originated this change, and should be the notified source for further
         *               messages. Can be {@code null} if there was none and the operation shouldn't send feedback.
         * @param changedRule The {@link CarpetRule} that changed. Use {@link CarpetRule#value() changedRule.value()} to get the rule's value,
         *                    and pass it to {@link RuleHelper#toRuleString(Object)} to get the {@link String} version of it
         */
        void ruleChanged(CommandSourceStack source, CarpetRule<?> changedRule); //TODO should this get value? If so need to change smth for addObserver
    }

    /**
     * Creates a new {@link SettingsManager} without a fancy name.
     * @see #SettingsManager(String, String, String)
     * 
     * @param version A {@link String} with the mod's version
     * @param identifier A {@link String} with the mod's id, will be the command name
     */
    public SettingsManager(String version, String identifier)
    {
        this(version, identifier, identifier);
    }

    /**
     * Creates a new {@link SettingsManager} with a fancy name
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
     * <p>Adds a custom rule observer to changes in rules from 
     * <b>this specific</b> {@link SettingsManager} instance.</p>
     * 
     * @see SettingsManager#addGlobalRuleObserver(RuleObserver)
     * 
     * @param observer A {@link RuleObserver} that will be called with
     *                 the used {@link CommandSourceStack} and the changed
     *                 {@link CarpetRule}.
     */
    public void addRuleObserver(RuleObserver observer)
    {
        observers.add(observer);
    }
    
    /**
     * Adds a custom rule observer to changes in rules from 
     * <b>any</b> {@link SettingsManager} instance (unless their implementation disallows it).
     * @see SettingsManager#addRuleObserver(TriConsumer)
     * 
     * @param observer A {@link RuleObserver} that will be called with
     *                 the used {@link CommandSourceStack}, and the changed
     *                 {@link CarpetRule}.
     */
    public static void addGlobalRuleObserver(RuleObserver observer)
    {
        staticObservers.add(observer);
    }

    /**
     * <p>Adds a custom rule observer to changes in rules from 
     * <b>this specific</b> {@link SettingsManager} instance.</p>
     * 
     * <p>This <b>outdated</b> method may not be able to listen to all {@link CarpetRule} implementations</p>
     * 
     * @see SettingsManager#addGlobalRuleObserver(RuleObserver)
     * 
     * @param observer A {@link TriConsumer} that will be called with
     *                 the used {@link CommandSourceStack}, the changed
     *                 {@link ParsedRule} and a {@link String} being the
     *                 value that the user typed.
     * @deprecated Use {@link SettingsManager#addRuleObserver(RuleObserver)} instead.
     */
    @Deprecated(forRemoval = true) //to remove
    public void addRuleObserver(TriConsumer<CommandSourceStack, ParsedRule<?>, String> observer)
    {
        observers.add((source, rule) -> {
            if (rule instanceof ParsedRule<?> pr)
                observer.accept(source, pr, RuleHelper.toRuleString(rule.value()));
        });
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
     * resets its {@link CarpetRule}s to their default values.<br>
     * This is handled automatically by Carpet
     */
    public void detachServer()
    {
        for (CarpetRule<?> rule : rules.values()) RuleHelper.resetToDefault(rule, null);
        server = null;
    }

    /**
     * Adds all annotated fields with the {@link Rule} annotation 
     * to this {@link SettingsManager} in order to handle them. 
     * @param settingsClass The class that will be analyzed
     */
    public void parseSettingsClass(Class<?> settingsClass)
    {
        nextRule: for (Field field : settingsClass.getDeclaredFields())
        {
            Rule annotation = field.getAnnotation(Rule.class);
            if (annotation == null) continue;
            CarpetRule<?> parsed = ParsedRule.of(field, annotation, this);
            for (Class<? extends Condition> condition : annotation.condition()) { //Should this be moved to ParsedRule.of?
                try
                {
                    Constructor<? extends Condition> constr = condition.getDeclaredConstructor();
                    constr.setAccessible(true);
                    if (!(constr.newInstance()).isTrue())
                        continue nextRule;
                }
                catch (ReflectiveOperationException e)
                {
                    throw new IllegalArgumentException(e);
                }
            }
            rules.put(parsed.name(), parsed);
        }
    }

    public void notifyRuleChanged(CommandSourceStack source, CarpetRule<?> rule) //TODO should this take a string? Or a T of new value? Or like this?
    {
        observers.forEach(observer -> observer.ruleChanged(source, rule));
        staticObservers.forEach(observer -> observer.ruleChanged(source, rule));
        ServerNetworkHandler.updateRuleWithConnectedClients(rule);
        switchScarpetRuleIfNeeded(source, rule); //TODO move into rule
        if (CARPET_RULE_CHANGES.isNeeded()) CARPET_RULE_CHANGES.onCarpetRuleChanges(rule, source);
    }
    
    private void switchScarpetRuleIfNeeded(CommandSourceStack source, CarpetRule<?> carpetRule) //TODO remove. This should be handled by the rule
    {
        if (carpetRule instanceof ParsedRule<?> rule && !rule.scarpetApp.isEmpty())
        {
            if (RuleHelper.getBooleanValue(rule) || (rule.type() == String.class && !rule.value().equals("false")))
            {
                CarpetServer.scriptServer.addScriptHost(source, rule.scarpetApp, s -> canUseCommand(s, rule.value()), false, false, true, null);
            } else {
                CarpetServer.scriptServer.removeScriptHost(source, rule.scarpetApp, false, true);
            }
        }
    }
    
    /**
     * <p>Initializes Scarpet rules in this {@link SettingsManager}, if any.</p>
     * <p>This is handled automatically by Carpet.</p>
     * @deprecated This has never been public API and will SoonTM be handled by the rules themselves
     */
    @Deprecated(forRemoval = true)
    public void initializeScarpetRules() { //TODO try remove
        for (CarpetRule<?> rule : rules.values())
        {
            if (rule instanceof ParsedRule<?> pr && !pr.scarpetApp.isEmpty()) {
                switchScarpetRuleIfNeeded(server.createCommandSourceStack(), pr);
            }
        }
    }

    /**
     * @return An {@link Iterable} with all categories
     *         that the rules in this {@link SettingsManager} have.
     * @implNote This method doesn't cache the result, so each call loops through all rules and finds all present categories
     */
    public Iterable<String> getCategories()
    {
        return getCarpetRules().stream().map(CarpetRule::categories).<String>mapMulti(Collection::forEach).collect(Collectors.toSet());
    }

    /**
     * <p>Gets a registered rule in this {@link SettingsManager}.</p>
     * 
     * @param name The name of the rule to get
     * @return A {@link CarpetRule} with the provided name or {@code null} if none in this {@link SettingsManager} matches
     */
    public CarpetRule<?> getCarpetRule(String name)
    {
        return rules.get(name);
    }
    
    /**
     * @return An unmodifiable {@link Collection} of the registered rules in this {@link SettingsManager}.
     */
    public Collection<CarpetRule<?>> getCarpetRules()
    {
        return Collections.unmodifiableCollection(rules.values());
    }

    /**
     * <p>Adds a {@link CarpetRule} to this {@link SettingsManager}.</p>
     * 
     * <p>Useful when having different {@link CarpetRule} implementations instead of a class of {@code static},
     * annotated fields.</p>
     * 
     * @param rule The {@link CarpetRule} to add
     * @throws UnsupportedOperationException If a rule with that name is already present in this {@link SettingsManager}
     */
    public void addCarpetRule(CarpetRule<?> rule) {
        if (rules.containsKey(rule.name()))
            throw new UnsupportedOperationException(fancyName + " settings manager already contains a rule with that name!");
        rules.put(rule.name(), rule);
    }

    private Path getFile()
    {
        return server.getWorldPath(LevelResource.ROOT).resolve(identifier + ".conf");
    }
    
    private Collection<CarpetRule<?>> getRulesSorted()
    {
        return rules.values().stream().sorted().collect(Collectors.toList());
    }

    /**
     * <p>Gets a registered {@link ParsedRule} in this {@link SettingsManager} by its name.</p>
     * @param name The rule name
     * @return The {@link ParsedRule} with that name
     * @deprecated Use {@link #getCarpetRule(String)} instead. This method is not able to return rules not implemented by {@link ParsedRule}
     */
    @Deprecated(forRemoval = true)
    public ParsedRule<?> getRule(String name)
    {
        return rules.get(name) instanceof ParsedRule<?> pr ? pr : null;
    }

    /**
     * @return A {@link Collection} of the registered {@link ParsedRule}s in this {@link SettingsManager}.
     * @deprecated Use {@link #getCarpetRules()} instead. This method won't be able to return rules not implemented by {@link ParsedRule}
     */
    @Deprecated(forRemoval = true)
    public Collection<ParsedRule<?>> getRules()
    {
        var parsedRuleClass = ParsedRule.class;
        return List.of(rules.values().stream().filter(parsedRuleClass::isInstance).map(parsedRuleClass::cast).toArray(ParsedRule[]::new));
    }


    /**
     * Disables all {@link CarpetRule}s with the {@link RuleCategory#COMMAND} category,
     * called when the {@link SettingsManager} is {@link #locked}.
     */
    private void disableBooleanCommands()
    {
        for (CarpetRule<?> rule : rules.values())
        {
            if (!rule.categories().contains(RuleCategory.COMMAND))
                continue;
            try {
                if (rule.suggestions().contains("false"))
                    rule.set(server.createCommandSourceStack(), "false");
                else
                    CarpetSettings.LOG.warn("Couldn't disable command rule "+ rule.name() + ": it doesn't suggest false as a valid option");
            } catch (InvalidRuleValueException e) {
                throw new IllegalStateException(e); // contract of CarpetRule.suggestions()
            }
        }
    }

    private void writeSettingsToConf(ConfigFileData data)
    {
        if (locked)
            return;
        try (BufferedWriter fw = Files.newBufferedWriter(getFile()))
        {
            for (String comment : data.comments())
            {
                fw.write(comment);
                fw.newLine();
            }
            for (String key: data.ruleMap().keySet())
            {
                fw.write(key + " " + data.ruleMap().get(key));
                fw.newLine();
            }
        }
        catch (IOException e)
        {
            CarpetSettings.LOG.error("[CM]: failed write "+identifier+".conf config file", e);
        }
    }

    /**
     * Notifies all players that the commands changed by resending the command tree.
     */
    public void notifyPlayersCommandsChanged() //TODO move
    {
        if (server == null || server.getPlayerList() == null)
        {
            return;
        }
        server.tell(new TickTask(this.server.getTickCount(), () ->
        {
            try {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    server.getCommands().sendCommands(player);
                }
            }
            catch (NullPointerException ignored) {}
        }));
    }

    /**
     * Returns whether the {@link CommandSourceStack} can execute
     * a command given the required permission level, according to
     * Carpet's standard for permissions.
     * @param source The origin {@link CommandSourceStack}
     * @param commandLevel A {@link String} being the permission level (either 0-4, a 
     *                     {@link boolean} value or "ops".
     * @return Whether or not the {@link CommandSourceStack} meets the required level
     */
    public static boolean canUseCommand(CommandSourceStack source, Object commandLevel) //TODO fix docs
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

    /**
     * <p>Returns whether or not this {@link SettingsManager} is locked, and any rules in it should therefore not be
     * toggleable.</p>
     * @return {@code true} if this {@link SettingsManager} is locked
     */
    public boolean locked() {
        return locked;
    }

    private Collection<CarpetRule<?>> findStartupOverrides()
    {
        Set<String> defaults = readSettingsFromConf(getFile()).ruleMap().keySet();
        return rules.values().stream().filter(r -> defaults.contains(r.name())).
                sorted().collect(Collectors.toList());
    }

    private Collection<CarpetRule<?>> getNonDefault()
    {
        return rules.values().stream().filter(Predicate.not(RuleHelper::isInDefaultValue)).sorted().collect(Collectors.toList());
    }

    private void loadConfigurationFromConf()
    {
        for (CarpetRule<?> rule : rules.values()) RuleHelper.resetToDefault(rule, server.createCommandSourceStack());
        ConfigFileData conf = readSettingsFromConf(getFile());
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
                rules.get(key).set(server.createCommandSourceStack(), conf.ruleMap().get(key));
                CarpetSettings.LOG.info("[CM]: loaded setting " + key + " as " + conf.ruleMap().get(key) + " from " + identifier + ".conf");
            }
            catch (InvalidRuleValueException exc)
            {
                CarpetSettings.LOG.error("[CM Error]: Failed to load setting: "+key+", " + exc.getMessage());
            }
        }
        locked = conf.locked();
    }


    private ConfigFileData readSettingsFromConf(Path path)
    {
        try (BufferedReader reader = Files.newBufferedReader(path))
        {
            String line = "";
            boolean confLocked = false;
            Map<String, String> result = new HashMap<String, String>();
            List<String> comments = new ArrayList<>();
            while ((line = reader.readLine()) != null)
            {
                line = line.replaceAll("[\\r\\n]", "");
                if ("locked".equalsIgnoreCase(line))
                {
                    confLocked = true;
                }
                String[] fields = line.split("\\s+",2);
                boolean saveComments = path.equals(getFile());
                if (fields.length > 1)
                {
                    if (result.isEmpty() && fields[0].startsWith("#") || fields[1].startsWith("#"))
                    {
                        if (saveComments)
                            comments.add(line); // Don't copy default config comments
                        continue;
                    }
                    if (!rules.containsKey(fields[0]))
                    {
                        CarpetSettings.LOG.error("[CM]: "+fancyName+" Setting " + fields[0] + " is not a valid rule - ignoring...");
                        continue;
                    }
                    result.put(fields[0],fields[1]);
                }
            }
            return new ConfigFileData(result, confLocked, comments);
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
                        try (BufferedWriter fw = Files.newBufferedWriter(defaultsPath))
                        {
                            fw.write("# This is " + fancyName + "'s default configuration file");
                            fw.newLine();
                            fw.write("# Settings specified here will be used when a world doesn't have a config file, but they will be completely "
                                    + "ignored once the world has one.");
                            fw.newLine();
                        }
                    }
                    return readSettingsFromConf(defaultsPath);
                } catch (IOException e2) {
                    CarpetSettings.LOG.error("Exception when loading fallback default config: ", e2);
                }
            }
            return new ConfigFileData(new HashMap<>(), false, List.of());
        }
        catch (IOException e)
        {
            CarpetSettings.LOG.error("Exception while loading Carpet rules from config", e);
            return new ConfigFileData(new HashMap<>(), false, List.of());
        }
    }

    private Collection<CarpetRule<?>> getRulesMatching(String search) {
        String lcSearch = search.toLowerCase(Locale.ROOT);
        return rules.values().stream().filter(rule ->
        {
            if (rule.name().toLowerCase(Locale.ROOT).contains(lcSearch)) return true; // substring match, case insensitive
            for (String c : rule.categories()) if (c.equals(search)) return true; // category exactly, case sensitive
            return Sets.newHashSet(rule.description().toLowerCase(Locale.ROOT).split("\\W+")).contains(lcSearch); // contains full term in description, but case insensitive
        }).sorted().collect(Collectors.toUnmodifiableList());
    }

    /**
     * A method to pretty print in markdown (useful for Github wiki/readme) the current
     * registered rules for a category to the log. Contains the name, description,
     * categories, type, defaults, wether or not they are strict, their suggested
     * values, and the descriptions of their validators.
     * @param category A {@link String} being the category to print, {@link null} to print
     *                 all registered rules.
     * @return actually nothing, the int is just there for brigadier
     */
    @SuppressWarnings({"deprecation", "removal"}) // accesses ParsedRule internals to print more information
    public int printAllRulesToLog(String category)
    {
        PrintStream ps = System.out;
        ps.println("# "+fancyName+" Settings");
        for (CarpetRule<?> rule : new TreeMap<>(rules).values())
        {
            if (category != null && !rule.categories().contains(category))
                continue;
            ps.println("## " + rule.name());
            ps.println(rule.description()+"  ");
            for (String extra : rule.extraInfo())
                ps.println(extra + "  ");
            ps.println("* Type: `" + rule.type().getSimpleName() + "`  ");
            ps.println("* Default value: `" + RuleHelper.toRuleString(rule.defaultValue()) + "`  ");
            String optionString = rule.suggestions().stream().map(s -> "`" + s + "`").collect(Collectors.joining(", "));
            if (!optionString.isEmpty()) ps.println((rule instanceof ParsedRule<?> pr && pr.isStrict?"* Required":"* Suggested")+" options: " + optionString + "  ");
            ps.println("* Categories: " + rule.categories().stream().map(s -> "`" + s.toUpperCase(Locale.ROOT) + "`").collect(Collectors.joining(", ")) + "  ");
            if (rule instanceof ParsedRule<?>)
            {
                boolean preamble = false;
                for (Validator<?> validator : ((ParsedRule<?>) rule).validators)
                {
                    if (validator.description() != null)
                    {
                        if (!preamble)
                        {
                            ps.println("* Additional notes:  ");
                            preamble = true;
                        }
                        ps.println("  * "+validator.description()+"  ");
                    }
                }
            }
            ps.println("  ");
        }
        return 1;
    }


    private CarpetRule<?> contextRule(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        String ruleName = StringArgumentType.getString(ctx, "rule");
        CarpetRule<?> rule = getCarpetRule(ruleName);
        if (rule == null)
            throw new SimpleCommandExceptionType(Messenger.c("rb "+ tr("ui.unknown_rule","Unknown rule")+": "+ruleName)).create();
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
                prefixes.add(String.join("", words.subList(i, words.size())));
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
                canUseCommand(player, CarpetSettings.carpetCommandPermissionLevel) && !locked());

        literalargumentbuilder.executes((context)-> listAllSettings(context.getSource())).
                then(literal("list").
                        executes( (c) -> listSettings(c.getSource(), String.format(tr("ui.all_%(mod)s_settings","All %s Settings"), fancyName),
                                getRulesSorted())).
                        then(literal("defaults").
                                executes( (c)-> listSettings(c.getSource(),
                                        String.format(tr("ui.current_%(mod)s_startup_settings_from_%(conf)s","Current %s Startup Settings from %s"), fancyName, (identifier+".conf")),
                                        findStartupOverrides()))).
                        then(argument("tag",StringArgumentType.word()).
                                suggests( (c, b)->suggest(getCategories(), b)).
                                executes( (c) -> listSettings(c.getSource(),
                                        String.format(tr("ui.%(mod)s_settings_matching_'%(query)s'","%s Settings matching \"%s\""), fancyName, tr("category." + StringArgumentType.getString(c, "tag"),StringArgumentType.getString(c, "tag"))),
                                        getRulesMatching(StringArgumentType.getString(c, "tag")))))).
                then(literal("removeDefault").
                        requires(s -> !locked()).
                        then(argument("rule", StringArgumentType.word()).
                                suggests( (c, b) -> suggestMatchingContains(getRulesSorted().stream().map(CarpetRule::name), b)).
                                executes((c) -> removeDefault(c.getSource(), contextRule(c))))).
                then(literal("setDefault").
                        requires(s -> !locked()).
                        then(argument("rule", StringArgumentType.word()).
                                suggests( (c, b) -> suggestMatchingContains(getRulesSorted().stream().map(CarpetRule::name), b)).
                                then(argument("value", StringArgumentType.greedyString()).
                                        suggests((c, b)-> suggest(contextRule(c).suggestions(), b)).
                                        executes((c) -> setDefault(c.getSource(), contextRule(c), StringArgumentType.getString(c, "value")))))).
                then(argument("rule", StringArgumentType.word()).
                        suggests( (c, b) -> suggestMatchingContains(getRulesSorted().stream().map(CarpetRule::name), b)).
                        requires(s -> !locked() ).
                        executes( (c) -> displayRuleMenu(c.getSource(), contextRule(c))).
                        then(argument("value", StringArgumentType.greedyString()).
                                suggests((c, b)-> suggest(contextRule(c).suggestions(),b)).
                                executes((c) -> setRule(c.getSource(), contextRule(c), StringArgumentType.getString(c, "value")))));

        dispatcher.register(literalargumentbuilder);
    }

    private int displayRuleMenu(CommandSourceStack source, CarpetRule<?> rule) //TODO check if there's dupe code around options buttons
    {
        String displayName = RuleHelper.translatedName(rule);

        Messenger.m(source, "");
        Messenger.m(source, "wb "+ displayName ,"!/"+identifier+" "+rule.name(),"^g refresh");
        Messenger.m(source, "w "+ RuleHelper.translatedDescription(rule));

        RuleHelper.translatedExtras(rule).forEach(s -> Messenger.m(source, "g  "+s));

        List<BaseComponent> tags = new ArrayList<>();
        tags.add(Messenger.c("w "+ tr("ui.tags", "Tags")+": "));
        for (String t: rule.categories())
        {
            String translated = tr("category." + t, t);
            tags.add(Messenger.c("c ["+ translated +"]", "^g "+ String.format(tr("list_all_%s_settings","list all %s settings"), translated),"!/"+identifier+" list "+t));
            tags.add(Messenger.c("w , "));
        }
        tags.remove(tags.size() - 1);
        Messenger.m(source, tags.toArray(new Object[0]));

        Messenger.m(source, "w "+ tr("ui.current_value", "Current value")+": ", String.format("%s %s (%s value)", RuleHelper.getBooleanValue(rule) ? "lb" : "nb", RuleHelper.toRuleString(rule.value()), RuleHelper.isInDefaultValue(rule) ? "default" : "modified"));
        List<BaseComponent> options = new ArrayList<>();
        options.add(Messenger.c("w Options: ", "y [ "));
        for (String o: rule.suggestions()) //TODO the todo is from around here
        {
            options.add(makeSetRuleButton(rule, o, false));
            options.add(Messenger.c("w  "));
        }
        options.remove(options.size()-1);
        options.add(Messenger.c("y  ]"));
        Messenger.m(source, options.toArray(new Object[0]));

        return 1;
    }

    private int setRule(CommandSourceStack source, CarpetRule<?> rule, String newValue)
    {
        try {
            rule.set(source, newValue);
            Messenger.m(source, "w "+rule.toString()+", ", "c ["+ tr("ui.change_permanently","change permanently")+"?]",
                    "^w "+String.format(tr("ui.click_to_keep_the_settings_in_%(conf)s_to_save_across_restarts","Click to keep the settings in %s to save across restarts"), identifier+".conf"),
                    "?/"+identifier+" setDefault "+rule.name()+" "+ RuleHelper.toRuleString(rule.value()));
        } catch (InvalidRuleValueException e) {
            e.notifySource(source);
        }
        return 1;
    }

    // stores different defaults in the file
    private int setDefault(CommandSourceStack source, CarpetRule<?> rule, String stringValue)
    {
        if (locked()) return 0;
        if (!rules.containsKey(rule.name())) return 0;
        ConfigFileData conf = readSettingsFromConf(getFile());
        conf.ruleMap().put(rule.name(), stringValue);
        writeSettingsToConf(conf); // this may feels weird, but if conf
        // is locked, it will never reach this point.
        try {
            rule.set(source, stringValue);
            Messenger.m(source ,"gi "+String.format(tr("ui.rule_%(rule)s_will_now_default_to_%(value)s","Rule %s will now default to %s"), RuleHelper.translatedName(rule), stringValue));
        } catch (InvalidRuleValueException e) {
            e.notifySource(source);
        }
        return 1;
    }
    // removes overrides of the default values in the file
    private int removeDefault(CommandSourceStack source, CarpetRule<?> rule)
    {
        if (locked) return 0;
        if (!rules.containsKey(rule.name())) return 0;
        ConfigFileData conf = readSettingsFromConf(getFile());
        conf.ruleMap().remove(rule.name());
        writeSettingsToConf(conf);
        RuleHelper.resetToDefault(rules.get(rule.name()), source);
        Messenger.m(source ,"gi "+String.format(tr("ui.rule_%(rule)s_not_set_restart","Rule %s will now not be set on restart"), RuleHelper.translatedName(rule)));
        return 1;
    }

    private BaseComponent displayInteractiveSetting(CarpetRule<?> rule)
    {
        String displayName = RuleHelper.translatedName(rule);
        List<Object> args = new ArrayList<>();
        args.add("w - "+ displayName +" ");
        args.add("!/"+identifier+" "+rule.name());
        args.add("^y "+RuleHelper.translatedDescription(rule));
        for (String option: rule.suggestions())
        {
            args.add(makeSetRuleButton(rule, option, true));
            args.add("w  ");
        }
        if (!rule.suggestions().contains(RuleHelper.toRuleString(rule.value())))
        {
            args.add(makeSetRuleButton(rule, RuleHelper.toRuleString(rule.value()), true));
            args.add("w  ");
        }
        args.remove(args.size()-1);
        return Messenger.c(args.toArray(new Object[0]));
    }

    private BaseComponent makeSetRuleButton(CarpetRule<?> rule, String option, boolean brackets)
    {
        String style = RuleHelper.isInDefaultValue(rule)?"g":(option.equalsIgnoreCase(RuleHelper.toRuleString(rule.defaultValue()))?"e":"y");
        if (option.equalsIgnoreCase(RuleHelper.toRuleString(rule.value())))
        {
            style = style + "u";
            if (option.equalsIgnoreCase(RuleHelper.toRuleString(rule.defaultValue())))
                style = style + "b";
        }
        String baseComponent = style + (brackets ? " [" : " ") + option + (brackets ? "]" : "");
        if (locked())
            return Messenger.c(baseComponent, "^g "+fancyName+" " + tr("ui.settings_are_locked","settings are locked"));
        if (option.equalsIgnoreCase(RuleHelper.toRuleString(rule.value())))
            return Messenger.c(baseComponent);
        return Messenger.c(baseComponent, "^g "+ tr("ui.switch_to","Switch to") +" " + option+(option.equalsIgnoreCase(RuleHelper.toRuleString(rule.defaultValue()))?" (default)":""), "?/"+identifier+" " + rule.name() + " " + option);
    }

    private int listSettings(CommandSourceStack source, String title, Collection<CarpetRule<?>> settings_list)
    {

        Messenger.m(source,String.format("wb %s:",title));
        settings_list.forEach(e -> Messenger.m(source, displayInteractiveSetting(e)));
        return settings_list.size();
    }
    private int listAllSettings(CommandSourceStack source)
    {
        int count = listSettings(source, String.format(tr("ui.current_%(mod)s_settings","Current %s Settings"), fancyName), getNonDefault());

        if (version != null)
            Messenger.m(source, "g "+fancyName+" "+ tr("ui.version",  "version") + ": "+ version);

        List<String> tags = new ArrayList<>();
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

    public void inspectClientsideCommand(CommandSourceStack source, String string)
    {
        if (string.startsWith("/" + identifier + " "))
        {
            String[] res = string.split("\\s+", 3);
            if (res.length == 3)
            {
                String setting = res[1];
                String strOption = res[2];
                if (rules.containsKey(setting) && rules.get(setting).canBeToggledClientSide())
                {
                    try {
                        rules.get(setting).set(source, strOption);
                    } catch (InvalidRuleValueException e) {
                        e.notifySource(source);
                    }
                }
            }
        }
    }
}
