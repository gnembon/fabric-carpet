package carpet.api.settings;

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
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Sets;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.api.settings.Rule.Condition;
import carpet.network.ServerNetworkHandler;
import carpet.settings.ParsedRule;
import carpet.utils.CommandHelper;
import carpet.utils.Messenger;
import carpet.utils.TranslationKeys;
import carpet.utils.Translations;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import static carpet.script.CarpetEventServer.Event.CARPET_RULE_CHANGES;
import static carpet.utils.Translations.tr;
import static java.util.Comparator.comparing;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.SharedSuggestionProvider.suggest;

/**
 * <p>A {@link SettingsManager} is a class that manages {@link CarpetRule carpet rules} in a {@link MinecraftServer},
 * including a command to manage it, and allowing other code to hook into rule changes by using {@link RuleObserver rule observers}.</p>
 * 
 * <p>You can create your own {@link SettingsManager} if you want to have your own command that will be handled the same way as Carpet's
 * by using {@link #SettingsManager(String, String, String)} and returning it in your {@link CarpetExtension}'s
 * {@link CarpetExtension#extensionSettingsManager()} method.</p>
 */
@SuppressWarnings({"deprecation", "removal"}) // remove after removing old system
public class SettingsManager {
    private final Map<String, CarpetRule<?>> rules = new HashMap<>();
    private final String version;
    private final String identifier;
    private final String fancyName;
    private boolean locked;
    private MinecraftServer server;
    private final List<RuleObserver> observers = new ArrayList<>();
    private static final List<RuleObserver> staticObservers = new ArrayList<>();
    static record ConfigReadResult(Map<String, String> ruleMap, boolean locked) {}
    
    /**
     * <p>Defines a class that can be notified about a {@link CarpetRule} changing.</p>
     * 
     * @see #ruleChanged(CommandSourceStack, CarpetRule, String)
     * @see SettingsManager#registerRuleObserver(RuleObserver)
     * @see SettingsManager#registerGlobalRuleObserver(RuleObserver)
     */
    @FunctionalInterface
    public static interface RuleObserver {
        /**
         * <p>Notifies this {@link RuleObserver} about the change of a {@link CarpetRule}.</p>
         * 
         * <p>When this is called, the {@link CarpetRule} value has already been set.</p>
         * 
         * @param source The {@link CommandSourceStack} that likely originated this change, and should be the notified source for further
         *               messages. Can be {@code null} if there was none and the operation shouldn't send feedback.
         * @param changedRule The {@link CarpetRule} that changed. Use {@link CarpetRule#value() changedRule.value()} to get the rule's value,
         *                    and pass it to {@link RuleHelper#toRuleString(Object)} to get the {@link String} version of it
         * @param userInput The {@link String} that the user entered when changing the rule, or a best-effort representation of it in case that is
         *                  is not available at the time (such as loading from disk or a rule being changed programmatically). Note that this value
         *                  may not represent the same string as converting the current value to a {@link String} via {@link RuleHelper#toRuleString(Object)},
         *                  given the rule implementation may have adapted the input into a different value, for example with the use of a {@link Validator}
         */
        void ruleChanged(CommandSourceStack source, CarpetRule<?> changedRule, String userInput);
    }
    
    /**
     * Creates a new {@link SettingsManager} with the given version, identifier and fancy name
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
     * <p>Registers a {@link RuleObserver} to changes in rules from 
     * this {@link SettingsManager} instance.</p>
     * 
     * @see SettingsManager#registerGlobalRuleObserver(RuleObserver)
     * 
     * @param observer A {@link RuleObserver} that will be called with
     *                 the used {@link CommandSourceStack} and the changed
     *                 {@link CarpetRule}.
     */
    public void registerRuleObserver(RuleObserver observer)
    {
        observers.add(observer);
    }
    
    /**
     * Registers a {@link RuleObserver} to changes in rules from 
     * <b>any</b> {@link SettingsManager} instance (unless their implementation disallows it).
     * @see SettingsManager#registerRuleObserver(RuleObserver)
     * 
     * @param observer A {@link RuleObserver} that will be called with
     *                 the used {@link CommandSourceStack}, and the changed
     *                 {@link CarpetRule}.
     */
    public static void registerGlobalRuleObserver(RuleObserver observer)
    {
        staticObservers.add(observer);
    }

    /**
     * @return A {@link String} being this {@link SettingsManager}'s 
     *         identifier, which is also the command name
     */
    public String identifier() {
        return identifier;
    }

    /**
     * <p>Returns whether this {@link SettingsManager} is locked, and any rules in it should therefore not be
     * toggleable and its management command should not be available.</p>
     * @return {@code true} if this {@link SettingsManager} is locked
     */
    public boolean locked() {
        return locked;
    }

    /**
     * Adds all annotated fields with the {@link Rule} annotation 
     * to this {@link SettingsManager} in order to handle them. 
     * @param settingsClass The class that will be analyzed
     */
    public void parseSettingsClass(Class<?> settingsClass)
    {
        // In the current translation system languages are not loaded this early. Ensure they are loaded
        Translations.updateLanguage();
        boolean warned = settingsClass == CarpetSettings.class; // don't warn for ourselves

        nextRule: for (Field field : settingsClass.getDeclaredFields())
        {
            Class<? extends Rule.Condition>[] conditions;
            Rule newAnnotation = field.getAnnotation(Rule.class);
            carpet.settings.Rule oldAnnotation = field.getAnnotation(carpet.settings.Rule.class);
            if (newAnnotation != null) {
                conditions = newAnnotation.conditions();
            } else if (oldAnnotation != null) {
                conditions = oldAnnotation.condition();
                if (!warned) {
                    CarpetSettings.LOG.warn("""
                        Registering outdated rules for settings class '%s'!
                        This won't be supported in the future and rules won't be registered!""".formatted(settingsClass.getName()));
                    warned = true;
                }
            } else {
                continue;
            }
            for (Class<? extends Rule.Condition> condition : conditions) { //Should this be moved to ParsedRule.of?
                try
                {
                    Constructor<? extends Condition> constr = condition.getDeclaredConstructor();
                    constr.setAccessible(true);
                    if (!(constr.newInstance()).shouldRegister())
                        continue nextRule;
                }
                catch (ReflectiveOperationException e)
                {
                    throw new IllegalArgumentException(e);
                }
            }
            CarpetRule<?> parsed = ParsedRule.of(field, this);
            rules.put(parsed.name(), parsed);
        }
    }

    /**
     * @return A String {@link Iterable} with all categories
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

    public void notifyRuleChanged(CommandSourceStack source, CarpetRule<?> rule, String userInput)
    {
        observers.forEach(observer -> observer.ruleChanged(source, rule, userInput));
        staticObservers.forEach(observer -> observer.ruleChanged(source, rule, userInput));
        ServerNetworkHandler.updateRuleWithConnectedClients(rule);
        switchScarpetRuleIfNeeded(source, rule); //TODO move into rule
        if (CARPET_RULE_CHANGES.isNeeded()) CARPET_RULE_CHANGES.onCarpetRuleChanges(rule, source);
    }

    /**
     * Attaches a {@link MinecraftServer} to this {@link SettingsManager}.<br>
     * This is handled automatically by Carpet and calling it manually is not supported.
     * 
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
     * This is handled automatically by Carpet and calling it manually is not supported.
     */
    public void detachServer()
    {
        for (CarpetRule<?> rule : rules.values()) RuleHelper.resetToDefault(rule, null);
        server = null;
    }
    
    /**
     * <p>Initializes Scarpet rules in this {@link SettingsManager}, if any.</p>
     * <p>This is handled automatically by Carpet and calling it is not supported.</p>
     */
    public void initializeScarpetRules() { //TODO try remove
        for (CarpetRule<?> rule : rules.values())
        {
            if (rule instanceof ParsedRule<?> pr && !pr.scarpetApp.isEmpty()) {
                switchScarpetRuleIfNeeded(server.createCommandSourceStack(), pr);
            }
        }
    }

    /**
     * Calling this method is not supported.
     */
    public void inspectClientsideCommand(CommandSourceStack source, String string)
    {
        if (string.startsWith("/" + identifier + " "))
        {
            String[] res = string.split("\\s+", 3);
            if (res.length == 3)
            {
                String rule = res[1];
                String strOption = res[2];
                if (rules.containsKey(rule) && rules.get(rule).canBeToggledClientSide())
                {
                    try {
                        rules.get(rule).set(source, strOption);
                    } catch (InvalidRuleValueException e) {
                        e.notifySource(rule, source);
                    }
                }
            }
        }
    }
    
    private void switchScarpetRuleIfNeeded(CommandSourceStack source, CarpetRule<?> carpetRule) //TODO remove. This should be handled by the rule
    {
        if (carpetRule instanceof ParsedRule<?> rule && !rule.scarpetApp.isEmpty() && CarpetServer.scriptServer != null) // null check because we may be in server init
        {
            if (RuleHelper.getBooleanValue(rule) || (rule.type() == String.class && !rule.value().equals("false")))
            {
                CarpetServer.scriptServer.addScriptHost(source, rule.scarpetApp, s -> CommandHelper.canUseCommand(s, rule.value()), false, false, true, null);
            } else {
                CarpetServer.scriptServer.removeScriptHost(source, rule.scarpetApp, false, true);
            }
        }
    }

    private Path getFile()
    {
        return server.getWorldPath(LevelResource.ROOT).resolve(identifier + ".conf");
    }
    
    private Collection<CarpetRule<?>> getRulesSorted()
    {
        return rules.values().stream().sorted(comparing(CarpetRule::name)).toList();
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

    private void writeSettingsToConf(ConfigReadResult data)
    {
        if (locked)
            return;
        try (BufferedWriter fw = Files.newBufferedWriter(getFile()))
        {
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

    private Collection<CarpetRule<?>> findStartupOverrides()
    {
        Set<String> defaults = readSettingsFromConf(getFile()).ruleMap().keySet();
        return rules.values().stream().filter(r -> defaults.contains(r.name())).
                sorted(comparing(CarpetRule::name)).toList();
    }

    private Collection<CarpetRule<?>> getNonDefault()
    {
        return rules.values().stream().filter(Predicate.not(RuleHelper::isInDefaultValue)).sorted(comparing(CarpetRule::name)).toList();
    }

    private void loadConfigurationFromConf()
    {
        for (CarpetRule<?> rule : rules.values()) RuleHelper.resetToDefault(rule, server.createCommandSourceStack());
        ConfigReadResult conf = readSettingsFromConf(getFile());
        locked = false;
        if (conf.locked())
        {
            CarpetSettings.LOG.info("[CM]: "+fancyName+" features are locked by the administrator");
            disableBooleanCommands();
        }
        int loadedCount = 0;
        for (String key: conf.ruleMap().keySet())
        {
            try
            {
                rules.get(key).set(server.createCommandSourceStack(), conf.ruleMap().get(key));
                loadedCount++;
            }
            catch (InvalidRuleValueException exc)
            {
                CarpetSettings.LOG.error("[CM Error]: Failed to load setting: " + key, exc);
            }
        }
        if (loadedCount > 0)
            CarpetSettings.LOG.info("[CM] Loaded " + loadedCount + " settings from " + identifier + ".conf");
        locked = conf.locked();
    }


    private ConfigReadResult readSettingsFromConf(Path path)
    {
        try (BufferedReader reader = Files.newBufferedReader(path))
        {
            String line = "";
            boolean confLocked = false;
            Map<String, String> result = new HashMap<String, String>();
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
                    if (result.isEmpty() && fields[0].startsWith("#") || fields[1].startsWith("#"))
                    {
                        continue;
                    }
                    if (!rules.containsKey(fields[0]))
                    {
                        CarpetSettings.LOG.error("[CM]: "+fancyName+" Setting " + fields[0] + " is not a valid rule - ignoring...");
                        continue;
                    }
                    result.put(fields[0], fields[1]);
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
            return new ConfigReadResult(new HashMap<>(), false);
        }
        catch (IOException e)
        {
            CarpetSettings.LOG.error("Exception while loading Carpet rules from config", e);
            return new ConfigReadResult(new HashMap<>(), false);
        }
    }

    private Collection<CarpetRule<?>> getRulesMatching(String search) {
        String lcSearch = search.toLowerCase(Locale.ROOT);
        return rules.values().stream().filter(rule ->
        {
            if (rule.name().toLowerCase(Locale.ROOT).contains(lcSearch)) return true; // substring match, case insensitive
            for (String c : rule.categories()) if (c.equals(search)) return true; // category exactly, case sensitive
            return Sets.newHashSet(RuleHelper.translatedDescription(rule).toLowerCase(Locale.ROOT).split("\\W+")).contains(lcSearch); // contains full term in description, but case insensitive
        }).sorted(comparing(CarpetRule::name)).toList();
    }

    /**
     * A method to pretty print in markdown (useful for Github wiki/readme) the current
     * registered rules for a category to the log. Contains the name, description,
     * categories, type, defaults, whether or not they are strict, their suggested
     * values, etc.
     * 
     * @param ps A {@link PrintStream} to dump the rules to, such as {@link System#out}
     * @param category A {@link String} being the category to print, {@link null} to print
     *                 all registered rules.
     * @return actually nothing, the int is just there for brigadier
     * 
     * @apiNote While extensions are expected to be able to call this method, binary compatibility isn't
     *          guaranteed, but this shouldn't be an issue given this is only intended to be ran for doc 
     *          generation (where version is controlled) and it's not expected to be referenced in production anyway
     */
    public int dumpAllRulesToStream(PrintStream ps, String category)
    {
        ps.println("# "+fancyName+" Settings");
        for (CarpetRule<?> rule : new TreeMap<>(rules).values())
        {
            if (category != null && !rule.categories().contains(category))
                continue;
            ps.println("## " + rule.name());
            ps.println(RuleHelper.translatedDescription(rule)+"  ");
            for (Component extra : rule.extraInfo())
                ps.println(extra.getString() + "  ");
            ps.println("* Type: `" + rule.type().getSimpleName() + "`  ");
            ps.println("* Default value: `" + RuleHelper.toRuleString(rule.defaultValue()) + "`  ");
            String options = rule.suggestions().stream().map(s -> "`" + s + "`").collect(Collectors.joining(", "));
            if (!options.isEmpty()) ps.println((rule.strict() ? "* Allowed" : "* Suggested") + " options: " + options + "  ");
            ps.println("* Categories: " + rule.categories().stream().map(s -> "`" + s.toUpperCase(Locale.ROOT) + "`").collect(Collectors.joining(", ")) + "  ");
            if (rule instanceof ParsedRule<?>)
            {
                boolean preamble = false;
                for (Validator<?> validator : ((ParsedRule<?>) rule).realValidators)
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
            throw new SimpleCommandExceptionType(Messenger.c("rb "+ tr(TranslationKeys.UNKNOWN_RULE) + ": "+ruleName)).create();
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
     * Registers the settings command for this {@link SettingsManager}.<br>
     * It is handled automatically by Carpet.
     * @param dispatcher The current {@link CommandDispatcher}
     * @param commandBuildContext The current {@link CommandBuildContext}
     */
    public void registerCommand(final CommandDispatcher<CommandSourceStack> dispatcher, final CommandBuildContext commandBuildContext)
    {
        if (dispatcher.getRoot().getChildren().stream().anyMatch(node -> node.getName().equalsIgnoreCase(identifier)))
        {
            CarpetSettings.LOG.error("Failed to add settings command for " + identifier + ". It is masking previous command.");
            return;
        }

        LiteralArgumentBuilder<CommandSourceStack> literalargumentbuilder = literal(identifier).requires((player) ->
                CommandHelper.canUseCommand(player, CarpetSettings.carpetCommandPermissionLevel) && !locked());

        literalargumentbuilder.executes((context)-> listAllSettings(context.getSource())).
                then(literal("list").
                        executes( (c) -> listSettings(c.getSource(), String.format(tr(TranslationKeys.ALL_MOD_SETTINGS), fancyName),
                                getRulesSorted())).
                        then(literal("defaults").
                                executes( (c)-> listSettings(c.getSource(),
                                        String.format(tr(TranslationKeys.CURRENT_FROM_FILE_HEADER), fancyName, (identifier+".conf")),
                                        findStartupOverrides()))).
                        then(argument("tag",StringArgumentType.word()).
                                suggests( (c, b)->suggest(getCategories(), b)).
                                executes( (c) -> listSettings(c.getSource(),
                                        String.format(tr(TranslationKeys.MOD_SETTINGS_MATCHING), fancyName, RuleHelper.translatedCategory(identifier(),StringArgumentType.getString(c, "tag"))),
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

        rule.extraInfo().forEach(s -> Messenger.m(source, s));

        List<Component> tags = new ArrayList<>();
        tags.add(Messenger.c("w "+ tr(TranslationKeys.TAGS)+": "));
        for (String t: rule.categories())
        {
            String translated = RuleHelper.translatedCategory(identifier(), t);
            tags.add(Messenger.c("c ["+ translated +"]", "^g "+ String.format(tr(TranslationKeys.LIST_ALL_CATEGORY), translated),"!/"+identifier+" list "+t));
            tags.add(Messenger.c("w , "));
        }
        tags.remove(tags.size() - 1);
        Messenger.m(source, tags.toArray(new Object[0]));

        Messenger.m(source, "w "+ tr(TranslationKeys.CURRENT_VALUE)+": ", String.format("%s %s (%s value)", RuleHelper.getBooleanValue(rule) ? "lb" : "nb", RuleHelper.toRuleString(rule.value()), RuleHelper.isInDefaultValue(rule) ? "default" : "modified"));
        List<Component> options = new ArrayList<>();
        options.add(Messenger.c("w Options: ", "y [ "));
        for (String o: rule.suggestions())
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
            Messenger.m(source, "w "+rule.toString()+", ", "c ["+ tr(TranslationKeys.CHANGE_PERMANENTLY)+"?]",
                    "^w "+String.format(tr(TranslationKeys.CHANGE_PERMANENTLY_HOVER), identifier+".conf"),
                    "?/"+identifier+" setDefault "+rule.name()+" "+ RuleHelper.toRuleString(rule.value()));
        } catch (InvalidRuleValueException e) {
            e.notifySource(rule.name(), source);
        }
        return 1;
    }

    // stores different defaults in the file
    private int setDefault(CommandSourceStack source, CarpetRule<?> rule, String stringValue)
    {
        if (locked()) return 0;
        if (!rules.containsKey(rule.name())) return 0;
        ConfigReadResult conf = readSettingsFromConf(getFile());
        conf.ruleMap().put(rule.name(), stringValue);
        writeSettingsToConf(conf); // this may feels weird, but if conf
        // is locked, it will never reach this point.
        try {
            rule.set(source, stringValue);
            Messenger.m(source ,"gi "+String.format(tr(TranslationKeys.DEFAULT_SET), RuleHelper.translatedName(rule), stringValue));
        } catch (InvalidRuleValueException e) {
            e.notifySource(rule.name(), source);
        }
        return 1;
    }
    // removes overrides of the default values in the file
    private int removeDefault(CommandSourceStack source, CarpetRule<?> rule)
    {
        if (locked) return 0;
        if (!rules.containsKey(rule.name())) return 0;
        ConfigReadResult conf = readSettingsFromConf(getFile());
        conf.ruleMap().remove(rule.name());
        writeSettingsToConf(conf);
        RuleHelper.resetToDefault(rules.get(rule.name()), source);
        Messenger.m(source ,"gi "+String.format(tr(TranslationKeys.DEFAULT_REMOVED), RuleHelper.translatedName(rule)));
        return 1;
    }

    private Component displayInteractiveSetting(CarpetRule<?> rule)
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

    private Component makeSetRuleButton(CarpetRule<?> rule, String option, boolean brackets)
    {
        String style = RuleHelper.isInDefaultValue(rule)?"g":(option.equalsIgnoreCase(RuleHelper.toRuleString(rule.defaultValue()))?"e":"y");
        if (option.equalsIgnoreCase(RuleHelper.toRuleString(rule.value())))
        {
            style = style + "u";
            if (option.equalsIgnoreCase(RuleHelper.toRuleString(rule.defaultValue())))
                style = style + "b";
        }
        String component = style + (brackets ? " [" : " ") + option + (brackets ? "]" : "");
        if (option.equalsIgnoreCase(RuleHelper.toRuleString(rule.value())))
            return Messenger.c(component);
        return Messenger.c(component, "^g "+ tr(TranslationKeys.SWITCH_TO).formatted(option + (option.equals(RuleHelper.toRuleString(rule.defaultValue()))?" (default)":"")), "?/"+identifier+" " + rule.name() + " " + option);
    }

    private int listSettings(CommandSourceStack source, String title, Collection<CarpetRule<?>> settings_list)
    {

        Messenger.m(source,String.format("wb %s:",title));
        settings_list.forEach(e -> Messenger.m(source, displayInteractiveSetting(e)));
        return settings_list.size();
    }
    private int listAllSettings(CommandSourceStack source)
    {
        int count = listSettings(source, String.format(tr(TranslationKeys.CURRENT_SETTINGS_HEADER), fancyName), getNonDefault());

        if (version != null)
            Messenger.m(source, "g "+fancyName+" "+ tr(TranslationKeys.VERSION) + ": " + version);

        List<String> tags = new ArrayList<>();
        tags.add("w " + tr(TranslationKeys.BROWSE_CATEGORIES)  + ":\n");
        for (String t : getCategories())
        {
            String translated = RuleHelper.translatedCategory(identifier(), t);
            String translatedPlus = !translated.equals(t) ? "%s (%s)".formatted(translated, t) : t;
            tags.add("c [" + translated +"]");
            tags.add("^g " + String.format(tr(TranslationKeys.LIST_ALL_CATEGORY), translatedPlus));
            tags.add("!/"+identifier+" list " + t);
            tags.add("w  ");
        }
        tags.remove(tags.size() - 1);
        Messenger.m(source, tags.toArray(new Object[0]));

        return count;
    }
}
