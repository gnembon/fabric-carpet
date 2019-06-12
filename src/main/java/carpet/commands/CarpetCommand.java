package carpet.commands;

import carpet.CarpetServer;
import carpet.settings.CarpetSettings;
import carpet.settings.ParsedRule;
import carpet.settings.SettingsManager;
import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.BaseText;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandSource.suggestMatching;

public class CarpetCommand
{
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher)
    {
        LiteralArgumentBuilder<ServerCommandSource> literalargumentbuilder = literal("carpet").requires((player) ->
                player.hasPermissionLevel(2) && !CarpetServer.settingsManager.locked);

        literalargumentbuilder.executes((context)->listAllSettings(context.getSource())).
                then(literal("list").
                        executes( (c) -> listSettings(c.getSource(),
                                "All CarpetMod Settings",
                                SettingsManager.getRules())).
                        then(literal("defaults").
                                executes( (c)-> listSettings(c.getSource(),
                                        "Current CarpetMod Startup Settings from carpet.conf",
                                        CarpetServer.settingsManager.findStartupOverrides()))).
                        then(argument("tag",StringArgumentType.word()).
                                suggests( (c, b)->suggestMatching(SettingsManager.getCategories(), b)).
                                executes( (c) -> listSettings(c.getSource(),
                                        String.format("CarpetMod Settings matching \"%s\"", StringArgumentType.getString(c, "tag")),
                                        CarpetServer.settingsManager.getRulesMatching(StringArgumentType.getString(c, "tag"))))));

        for (ParsedRule<?> rule: SettingsManager.getRules())
        {
            literalargumentbuilder.then(literal(rule.name).executes( (context) ->
                    displayRuleMenu(context.getSource(),rule)));
            literalargumentbuilder.then(literal("removeDefault").
                    requires(s -> !CarpetServer.settingsManager.locked).
                    then(literal(rule.name).executes((context) ->
                            removeDefault(context.getSource(), rule))));
            literalargumentbuilder.then(literal(rule.name).
                    requires(s -> !CarpetServer.settingsManager.locked).
                    then(argument("value", StringArgumentType.word()).
                            suggests((c, b)-> suggestMatching(rule.options,b)).
                            executes((context) ->
                                    setRule(context.getSource(), rule, StringArgumentType.getString(context, "value")))));
            literalargumentbuilder.then(literal("setDefault").
                    requires(s -> !CarpetServer.settingsManager.locked).
                    then(literal(rule.name).
                            then(argument("value", StringArgumentType.word()).
                                    suggests((c, b)-> suggestMatching(rule.options,b)).
                                    executes((context) ->
                                            setDefault(context.getSource(), rule, StringArgumentType.getString(context, "value"))))));
        }
        dispatcher.register(literalargumentbuilder);
    }

    private static int displayRuleMenu(ServerCommandSource source, ParsedRule<?> rule)
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
        Messenger.m(player, "wb "+rule.name,"!/carpet "+rule.name,"^g refresh");
        Messenger.m(player, "w "+rule.description);

        rule.extraInfo.forEach(s -> Messenger.m(player, "g  "+s));

        List<BaseText> tags = new ArrayList<>();
        tags.add(Messenger.c("w Tags: "));
        for (String t: rule.categories)
        {
            tags.add(Messenger.c("c ["+t+"]", "^g list all "+t+" settings","!/carpet list "+t));
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

    private static int setRule(ServerCommandSource source, ParsedRule<?> rule, String newValue)
    {
        if (rule.set(source, newValue) != null)
            Messenger.m(source, "w "+rule.toString()+", ", "c [change permanently?]",
                    "^w Click to keep the settings in carpet.conf to save across restarts",
                    "?/carpet setDefault "+rule.name+" "+rule.getAsString());
        return 1;
    }
    private static int setDefault(ServerCommandSource source, ParsedRule<?> rule, String defaultValue)
    {
        if (CarpetServer.settingsManager.setDefaultRule(source, rule.name, defaultValue))
            Messenger.m(source ,"gi rule "+ rule.name+" will now default to "+ defaultValue);
        return 1;
    }
    private static int removeDefault(ServerCommandSource source, ParsedRule<?> rule)
    {
        if (CarpetServer.settingsManager.removeDefaultRule(source, rule.name))
            Messenger.m(source ,"gi rule "+ rule.name+" defaults to Vanilla");
        return 1;
    }


    private static BaseText displayInteractiveSetting(ParsedRule<?> rule)
    {
        List<Object> args = new ArrayList<>();
        args.add("w - "+rule.name+" ");
        args.add("!/carpet "+rule.name);
        args.add("^y "+rule.description);
        for (String option: rule.options)
        {
            args.add(makeSetRuleButton(rule, option, true));
            args.add("w  ");
        }
        args.remove(args.size()-1);
        return Messenger.c(args.toArray(new Object[0]));
    }

    private static BaseText makeSetRuleButton(ParsedRule<?> rule, String option, boolean brackets)
    {
        String style = rule.isDefault()?"g":(option.equalsIgnoreCase(rule.defaultAsString)?"y":"e");
        if (option.equalsIgnoreCase(rule.defaultAsString))
            style = style+"b";
        else if (option.equalsIgnoreCase(rule.getAsString()))
            style = style+"u";
        String baseText = style + (brackets ? " [" : " ") + option + (brackets ? "]" : "");
        if (CarpetServer.settingsManager.locked)
            return Messenger.c(baseText, "^g Settings are locked");
        if (option.equalsIgnoreCase(rule.getAsString()))
            return Messenger.c(baseText);
        return Messenger.c(baseText, "^g Switch to " + option, "?/carpet " + rule.name + " " + option);
    }

    private static int listSettings(ServerCommandSource source, String title, Collection<ParsedRule<?>> settings_list)
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
    private static int listAllSettings(ServerCommandSource source)
    {
        listSettings(source, "Current CarpetMod Settings", CarpetServer.settingsManager.getNonDefault());

        Messenger.m(source, "Carpet Mod version: "+ CarpetSettings.carpetVersion);
        try
        {
            PlayerEntity player = source.getPlayer();
            List<Object> tags = new ArrayList<>();
            tags.add("w Browse Categories:\n");
            for (String t : SettingsManager.getCategories())
            {
                tags.add("c [" + t+"]");
                tags.add("^g list all " + t + " settings");
                tags.add("!/carpet list " + t);
                tags.add("w  ");
            }
            tags.remove(tags.size() - 1);
            Messenger.m(player, tags.toArray(new Object[0]));
        }
        catch (CommandSyntaxException ignored) { }
        return 1;
    }
}
