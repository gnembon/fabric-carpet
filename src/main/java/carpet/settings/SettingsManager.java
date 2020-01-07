package carpet.settings;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.utils.Messenger;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTask;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.BaseText;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.util.TriConsumer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
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

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandSource.suggestMatching;

public class SettingsManager
{
    private Map<String, ParsedRule<?>> rules = new HashMap<>();
    public boolean locked;
    private final String version;
    private final String identifier;
    private final String fancyName;
    private MinecraftServer server;
    private List<TriConsumer<ServerCommandSource, ParsedRule<?>, String>> observers = new ArrayList<>();

    public SettingsManager(String version, String identifier)
    {
        this.version = version;
        this.identifier = identifier;
        this.fancyName = identifier;
    }

    public SettingsManager(String version, String identifier, String fancyName)
    {
        this.version = version;
        this.identifier = identifier;
        this.fancyName = fancyName;
    }


    public void attachServer(MinecraftServer server)
    {
        this.server = server;
        loadConfigurationFromConf();
        registerCommand(server.getCommandManager().getDispatcher());
        notifyPlayersCommandsChanged();
    }

    public void detachServer()
    {
        for (ParsedRule<?> rule : rules.values()) rule.resetToDefault(server.getCommandSource());
        server = null;
    }

    public void parseSettingsClass(Class settingsClass)
    {
        for (Field f : settingsClass.getDeclaredFields())
        {
            Rule rule = f.getAnnotation(Rule.class);
            if (rule == null) continue;
            ParsedRule parsed = new ParsedRule(f, rule);
            rules.put(parsed.name, parsed);
        }
    }

    public void addRuleObserver(TriConsumer<ServerCommandSource, ParsedRule<?>, String> observer)
    {
        observers.add(observer);
    }

    void notifyRuleChanged(ServerCommandSource source, ParsedRule<?> rule, String userTypedValue)
    {
        observers.forEach(observer -> observer.accept(source, rule, userTypedValue));
    }

    public Iterable<String> getCategories()
    {
        Set<String> categories = new HashSet<>();
        getRules().stream().map(r -> r.categories).forEach(categories::addAll);
        return categories;
    }


    public ParsedRule<?> getRule(String name)
    {
        return rules.get(name);
    }

    public Collection<ParsedRule<?>> getRules()
    {
        return rules.values().stream().sorted().collect(Collectors.toList());
    }

    public Collection<ParsedRule<?>> findStartupOverrides()
    {
        Set<String> defaults = readSettingsFromConf().getLeft().keySet();
        return rules.values().stream().filter(r -> defaults.contains(r.name)).
                sorted().collect(Collectors.toList());
    }


    public Collection<ParsedRule<?>> getNonDefault()
    {
        return rules.values().stream().filter(r -> !r.isDefault()).sorted().collect(Collectors.toList());
    }

    private File getFile()
    {
        return server.getLevelStorage().resolveFile(server.getLevelName(), identifier+".conf");
    }

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
            CarpetSettings.LOG.error("[CM]: failed write "+identifier+".conf config file");
        }
        ///todo is it really needed? resendCommandTree();
    }

    public void notifyPlayersCommandsChanged()
    {
        if (server == null || server.getPlayerManager() == null)
        {
            return;
        }
        server.send(new ServerTask(this.server.getTicks(), () ->
        {
            for (ServerPlayerEntity entityplayermp : server.getPlayerManager().getPlayerList())
            {
                server.getCommandManager().sendCommandTree(entityplayermp);
            }
        }));
    }

    public static boolean canUseCommand(ServerCommandSource source, String commandLevel)
    {
        switch (commandLevel)
        {
            case "true": return true;
            case "false": return false;
            case "ops": return source.hasPermissionLevel(2); // typical for other cheaty commands
            case "0":
            case "1":
            case "2":
            case "3":
            case "4":
                return source.hasPermissionLevel(Integer.parseInt(commandLevel));
        }
        return false;
    }

    private void loadConfigurationFromConf()
    {
        for (ParsedRule<?> rule : rules.values()) rule.resetToDefault(server.getCommandSource());
        Pair<Map<String, String>,Boolean> conf = readSettingsFromConf();
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


    private Pair<Map<String, String>,Boolean> readSettingsFromConf()
    {
        try
        {
            BufferedReader reader = new BufferedReader(new FileReader(getFile()));
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

    private Collection<ParsedRule<?>> getRulesMatching(String search) {
        String lcSearch = search.toLowerCase(Locale.ROOT);
        return rules.values().stream().filter(rule ->
        {
            if (rule.name.toLowerCase(Locale.ROOT).contains(lcSearch)) return true; // substring match, case insensitive
            for (String c : rule.categories) if (c.equals(search)) return true; // category exactly, case sensitive
            return Sets.newHashSet(rule.description.toLowerCase(Locale.ROOT).split("\\W+")).contains(lcSearch); // contains full term in description, but case insensitive
        }).sorted().collect(ImmutableList.toImmutableList());
    }

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
            throw new SimpleCommandExceptionType(Messenger.c("rb Unknown rule: "+ruleName)).create();
        return rule;
    }

    private void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher)
    {
        if (dispatcher.getRoot().getChildren().stream().anyMatch(node -> node.getName().equalsIgnoreCase(identifier)))
        {
            Messenger.print_server_message(CarpetServer.minecraft_server,
                    Messenger.c("rb Failed to add settings command for " + identifier + ". It is masking previous command."));
            return;
        }

        LiteralArgumentBuilder<ServerCommandSource> literalargumentbuilder = literal(identifier).requires((player) ->
                player.hasPermissionLevel(2) && !locked);

        literalargumentbuilder.executes((context)->listAllSettings(context.getSource())).
                then(literal("list").
                        executes( (c) -> listSettings(c.getSource(), "All "+fancyName+" Settings",
                                getRules())).
                        then(literal("defaults").
                                executes( (c)-> listSettings(c.getSource(),
                                        "Current "+fancyName+" Startup Settings from "+identifier+".conf",
                                        findStartupOverrides()))).
                        then(argument("tag",StringArgumentType.word()).
                                suggests( (c, b)->suggestMatching(getCategories(), b)).
                                executes( (c) -> listSettings(c.getSource(),
                                        String.format(fancyName+" Settings matching \"%s\"", StringArgumentType.getString(c, "tag")),
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
        try
        {
            player = source.getPlayer();
        }
        catch (CommandSyntaxException e)
        {
            Messenger.m(source, "w "+rule.name +" is set to: ","wb "+rule.getAsString());
            return 1;
        }

        Messenger.m(player, "");
        Messenger.m(player, "wb "+rule.name,"!/"+identifier+" "+rule.name,"^g refresh");
        Messenger.m(player, "w "+rule.description);

        rule.extraInfo.forEach(s -> Messenger.m(player, "g  "+s));

        List<BaseText> tags = new ArrayList<>();
        tags.add(Messenger.c("w Tags: "));
        for (String t: rule.categories)
        {
            tags.add(Messenger.c("c ["+t+"]", "^g list all "+t+" settings","!/"+identifier+" list "+t));
            tags.add(Messenger.c("w , "));
        }
        tags.remove(tags.size()-1);
        Messenger.m(player, tags.toArray(new Object[0]));

        Messenger.m(player, "w Current value: ",String.format("%s %s (%s value)",rule.getBoolValue()?"lb":"nb", rule.getAsString(),rule.isDefault()?"default":"modified"));
        List<BaseText> options = new ArrayList<>();
        options.add(Messenger.c("w Options: ", "y [ "));
        for (String o: rule.options)
        {
            options.add(makeSetRuleButton(rule, o, false));
            options.add(Messenger.c("w  "));
        }
        options.remove(options.size()-1);
        options.add(Messenger.c("y  ]"));
        Messenger.m(player, options.toArray(new Object[0]));

        return 1;
    }

    private int setRule(ServerCommandSource source, ParsedRule<?> rule, String newValue)
    {
        if (rule.set(source, newValue) != null)
            Messenger.m(source, "w "+rule.toString()+", ", "c [change permanently?]",
                    "^w Click to keep the settings in "+identifier+".conf to save across restarts",
                    "?/"+identifier+" setDefault "+rule.name+" "+rule.getAsString());
        return 1;
    }

    // stores different defaults in the file
    private int setDefault(ServerCommandSource source, ParsedRule<?> rule, String stringValue)
    {
        if (locked) return 0;
        if (!rules.containsKey(rule.name)) return 0;
        Pair<Map<String, String>,Boolean> conf = readSettingsFromConf();
        conf.getLeft().put(rule.name, stringValue);
        writeSettingsToConf(conf.getLeft()); // this may feels weird, but if conf
        // is locked, it will never reach this point.
        rule.set(source,stringValue);
        Messenger.m(source ,"gi rule "+ rule.name+" will now default to "+ stringValue);
        return 1;
    }
    // removes overrides of the default values in the file
    private int removeDefault(ServerCommandSource source, ParsedRule<?> rule)
    {
        if (locked) return 0;
        if (!rules.containsKey(rule.name)) return 0;
        Pair<Map<String, String>,Boolean> conf = readSettingsFromConf();
        conf.getLeft().remove(rule.name);
        writeSettingsToConf(conf.getLeft());
        rules.get(rule.name).resetToDefault(source);
        return 1;
    }

    private BaseText displayInteractiveSetting(ParsedRule<?> rule)
    {
        List<Object> args = new ArrayList<>();
        args.add("w - "+rule.name+" ");
        args.add("!/"+identifier+" "+rule.name);
        args.add("^y "+rule.description);
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
            return Messenger.c(baseText, "^g "+fancyName+" settings are locked");
        if (option.equalsIgnoreCase(rule.getAsString()))
            return Messenger.c(baseText);
        return Messenger.c(baseText, "^g Switch to " + option+(option.equalsIgnoreCase(rule.defaultAsString)?" (default)":""), "?/"+identifier+" " + rule.name + " " + option);
    }

    private int listSettings(ServerCommandSource source, String title, Collection<ParsedRule<?>> settings_list)
    {
        try
        {
            PlayerEntity player = source.getPlayer();
            Messenger.m(player,String.format("wb %s:",title));
            settings_list.forEach(e -> Messenger.m(player,displayInteractiveSetting(e)));

        }
        catch (CommandSyntaxException e)
        {
            Messenger.m(source, "w s:"+title);
            settings_list.forEach(r -> Messenger.m(source, "w  - "+ r.toString()));
        }
        return 1;
    }
    private int listAllSettings(ServerCommandSource source)
    {
        listSettings(source, "Current "+fancyName+" Settings", getNonDefault());

        if (version != null)
            Messenger.m(source, "g "+fancyName+" version: "+ version);
        try
        {
            PlayerEntity player = source.getPlayer();
            List<Object> tags = new ArrayList<>();
            tags.add("w Browse Categories:\n");
            for (String t : getCategories())
            {
                tags.add("c [" + t+"]");
                tags.add("^g list all " + t + " settings");
                tags.add("!/"+identifier+" list " + t);
                tags.add("w  ");
            }
            tags.remove(tags.size() - 1);
            Messenger.m(player, tags.toArray(new Object[0]));
        }
        catch (CommandSyntaxException ignored) { }
        return 1;
    }

}
