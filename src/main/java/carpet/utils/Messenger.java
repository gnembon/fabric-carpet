package carpet.utils;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Messenger
{
    public static final Logger LOG = LogManager.getLogger("Messaging System");

    private static final Pattern colorExtract = Pattern.compile("#([0-9a-fA-F]{6})");
    public enum CarpetFormatting
    {
        ITALIC      ('i', (s, f) -> s.withItalic(true)),
        STRIKE      ('s', (s, f) -> s.applyFormat(ChatFormatting.STRIKETHROUGH)),
        UNDERLINE   ('u', (s, f) -> s.applyFormat(ChatFormatting.UNDERLINE)),
        BOLD        ('b', (s, f) -> s.withBold(true)),
        OBFUSCATE   ('o', (s, f) -> s.applyFormat(ChatFormatting.OBFUSCATED)),

        WHITE       ('w', (s, f) -> s.withColor(ChatFormatting.WHITE)),
        YELLOW      ('y', (s, f) -> s.withColor(ChatFormatting.YELLOW)),
        LIGHT_PURPLE('m', (s, f) -> s.withColor(ChatFormatting.LIGHT_PURPLE)), // magenta
        RED         ('r', (s, f) -> s.withColor(ChatFormatting.RED)),
        AQUA        ('c', (s, f) -> s.withColor(ChatFormatting.AQUA)), // cyan
        GREEN       ('l', (s, f) -> s.withColor(ChatFormatting.GREEN)), // lime
        BLUE        ('t', (s, f) -> s.withColor(ChatFormatting.BLUE)), // light blue, teal
        DARK_GRAY   ('f', (s, f) -> s.withColor(ChatFormatting.DARK_GRAY)),
        GRAY        ('g', (s, f) -> s.withColor(ChatFormatting.GRAY)),
        GOLD        ('d', (s, f) -> s.withColor(ChatFormatting.GOLD)),
        DARK_PURPLE ('p', (s, f) -> s.withColor(ChatFormatting.DARK_PURPLE)), // purple
        DARK_RED    ('n', (s, f) -> s.withColor(ChatFormatting.DARK_RED)),  // brown
        DARK_AQUA   ('q', (s, f) -> s.withColor(ChatFormatting.DARK_AQUA)),
        DARK_GREEN  ('e', (s, f) -> s.withColor(ChatFormatting.DARK_GREEN)),
        DARK_BLUE   ('v', (s, f) -> s.withColor(ChatFormatting.DARK_BLUE)), // navy
        BLACK       ('k', (s, f) -> s.withColor(ChatFormatting.BLACK)),

        COLOR       ('#', (s, f) -> {
            TextColor color = TextColor.parseColor("#"+f);
            return color == null ? s : s.withColor(color);
        }, s -> {
            Matcher m = colorExtract.matcher(s);
            return m.find() ? m.group(1) : null;
        }),
        ;

        public char code;
        public BiFunction<Style, String, Style> applier;
        public Function<String, String> container;
        CarpetFormatting(char code, BiFunction<Style, String, Style> applier)
        {
            this(code, applier, s -> s.indexOf(code)>=0?Character.toString(code):null);
        }
        CarpetFormatting(char code, BiFunction<Style, String, Style> applier, Function<String, String> container)
        {
            this.code = code;
            this.applier = applier;
            this.container = container;
        }
        public Style apply(String format, Style previous)
        {
            String fmt;
            if ((fmt = container.apply(format))!= null) return applier.apply(previous, fmt);
            return previous;
        }
    };

    public static Style parseStyle(String style)
    {
        Style myStyle= Style.EMPTY;
        for (CarpetFormatting cf: CarpetFormatting.values()) myStyle = cf.apply(style, myStyle);
        return myStyle;
    }
    public static String heatmap_color(double actual, double reference)
    {
        String color = "g";
        if (actual >= 0.0D) color = "e";
        if (actual > 0.5D*reference) color = "y";
        if (actual > 0.8D*reference) color = "r";
        if (actual > reference) color = "m";
        return color;
    }
    public static String creatureTypeColor(MobCategory type)
    {
        return switch (type)
        {
            case MONSTER -> "n";
            case CREATURE -> "e";
            case AMBIENT -> "f";
            case WATER_CREATURE -> "v";
            case WATER_AMBIENT -> "q";
            default -> "w"; // missing MISC and UNDERGROUND_WATER_CREATURE
        };
    }

    private static BaseComponent getChatComponentFromDesc(String message, BaseComponent previousMessage)
    {
        if (message.equalsIgnoreCase(""))
        {
            return new TextComponent("");
        }
        int limit = message.indexOf(' ');
        String desc = message;
        String str = "";
        if (limit >= 0)
        {
            desc = message.substring(0, limit);
            str = message.substring(limit + 1);
        }
        if (previousMessage == null)
        {
            return s(str, desc);
        }
        Style previousStyle = previousMessage.getStyle();
        BaseComponent ret = previousMessage;
        previousMessage.setStyle(switch (desc.isEmpty() ? ' ' : desc.charAt(0)) {
            case '?' -> previousStyle.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, message.substring(1)));
            case '!' -> previousStyle.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, message.substring(1)));
            case '^' -> previousStyle.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, c(message.substring(1))));
            case '@' -> previousStyle.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, message.substring(1)));
            case '&' -> previousStyle.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, message.substring(1)));
            default  -> { // Create a new component
                ret = s(str, desc);
                yield previousStyle; // no op for the previous style
            }
        });
        return ret;
    }
    private static BaseComponent processChatComponentFromDesc(BaseComponent message, String desc, BaseComponent previousMessage)
    {
        message = (BaseComponent) message.copy();
        message.setStyle(parseStyle(desc));
        if (previousMessage != null && desc.charAt(0) == '^')
        {
            previousMessage.setStyle(previousMessage.getStyle().withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, message)));
            return previousMessage;
        }
        return message;
    }
    public static BaseComponent tp(String desc, Vec3 pos) { return tp(desc, pos.x, pos.y, pos.z); }
    public static BaseComponent tp(String desc, BlockPos pos) { return tp(desc, pos.getX(), pos.getY(), pos.getZ()); }
    public static BaseComponent tp(String desc, double x, double y, double z) { return tp(desc, (float)x, (float)y, (float)z);}
    public static BaseComponent tp(String desc, float x, float y, float z)
    {
        return getCoordsTextComponent(desc, x, y, z, false);
    }
    public static BaseComponent tp(String desc, int x, int y, int z)
    {
        return getCoordsTextComponent(desc, (float)x, (float)y, (float)z, true);
    }

    /// to be continued
    public static BaseComponent dbl(String style, double double_value)
    {
        return c(String.format("%s %.1f",style,double_value),String.format("^w %f",double_value));
    }
    public static BaseComponent dbls(String style, double ... doubles)
    {
        StringBuilder str = new StringBuilder(style + " [ ");
        String prefix = "";
        for (double dbl : doubles)
        {
            str.append(String.format("%s%.1f", prefix, dbl));
            prefix = ", ";
        }
        str.append(" ]");
        return c(str.toString());
    }
    public static BaseComponent dblf(String style, double ... doubles)
    {
        StringBuilder str = new StringBuilder(style + " [ ");
        String prefix = "";
        for (double dbl : doubles)
        {
            str.append(String.format("%s%f", prefix, dbl));
            prefix = ", ";
        }
        str.append(" ]");
        return c(str.toString());
    }
    public static BaseComponent dblt(String style, double ... doubles)
    {
        List<Object> components = new ArrayList<>();
        components.add(style+" [ ");
        String prefix = "";
        for (double dbl:doubles)
        {

            components.add(String.format("%s %s%.1f",style, prefix, dbl));
            components.add("?"+dbl);
            components.add("^w "+dbl);
            prefix = ", ";
        }
        //components.remove(components.size()-1);
        components.add(style+"  ]");
        return c(components.toArray(new Object[0]));
    }

    private static BaseComponent getCoordsTextComponent(String style, float x, float y, float z, boolean isInt)
    {
        String text;
        String command;
        if (isInt)
        {
            text = String.format("%s [ %d, %d, %d ]",style, (int)x,(int)y, (int)z );
            command = String.format("!/tp %d %d %d",(int)x,(int)y, (int)z);
        }
        else
        {
            text = String.format("%s [ %.1f, %.1f, %.1f]",style, x, y, z);
            command = String.format("!/tp %.3f %.3f %.3f",x, y, z);
        }
        return c(text, command);
    }

    //message source
    public static void m(CommandSourceStack source, Object ... fields)
    {
        if (source != null)
            source.sendSuccess(Translations.tr(Messenger.c(fields), source),source.getServer() != null && source.getServer().getLevel(Level.OVERWORLD) != null); //OW
    }
    public static void m(Player player, Object ... fields)
    {
        player.sendMessage(Translations.tr(Messenger.c(fields), player), Util.NIL_UUID);
    }

    /*
    composes single line, multicomponent message, and returns as one chat messagge
     */
    public static BaseComponent c(Object ... fields)
    {
        BaseComponent message = new TextComponent("");
        BaseComponent previousComponent = null;
        String desc = null;
        for (Object o: fields)
        {
            BaseComponent comp;
            if (o instanceof BaseComponent baseComponent)
            {
                comp = baseComponent;
            }
            else
            {
                String txt = o.toString();
                if (txt.indexOf(' ') == -1)  // no space in txt, it's a descriptor for the next BaseComponent
                {
                    desc = txt;
                    continue;
                }
                comp = getChatComponentFromDesc(txt, previousComponent);
            }

            if (desc != null)
            {
                comp = processChatComponentFromDesc(comp, desc, previousComponent);
            }

            desc = null;
            if (comp != previousComponent)
            {
                message.append(comp);
                previousComponent = comp;
            }
        }
        return message;
    }

    //simple text

    public static BaseComponent s(String text)
    {
        return s(text,"");
    }
    public static BaseComponent s(String text, String style)
    {
        BaseComponent message = new TextComponent(text);
        message.setStyle(parseStyle(style));
        return message;
    }

    //translation text
    public static BaseComponent tr(String key, Object ... args)
    {
        return new TranslatableComponent(key, args);
    }



    public static void send(Player player, Collection<BaseComponent> lines)
    {
        lines.forEach(message -> player.sendMessage(Translations.tr(message, player), Util.NIL_UUID));
    }
    public static void send(CommandSourceStack source, Collection<BaseComponent> lines)
    {
        lines.forEach(message -> source.sendSuccess(Translations.tr(message, source), false));
    }


    public static void print_server_message(MinecraftServer server, String message)
    {
        print_server_message(server, c("gi "+message));
    }
    public static void print_server_message(MinecraftServer server, BaseComponent message)
    {
        if (server == null)
        {
            LOG.error("Message not delivered since server is null: " + message);
            return;
        }
        server.sendMessage(Translations.tr(message), Util.NIL_UUID);
        for (ServerPlayer entityplayer : server.getPlayerList().getPlayers())
        {
            entityplayer.sendMessage(Translations.tr(message, entityplayer), Util.NIL_UUID);
        }
    }
}

