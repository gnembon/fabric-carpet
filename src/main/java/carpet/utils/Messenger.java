package carpet.utils;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.BaseComponent;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Messenger
{
    public static final Logger LOG = LoggerFactory.getLogger("Messaging System");

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
        Style myStyle= Style.EMPTY.withColor(ChatFormatting.WHITE);
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
        if (Character.isWhitespace(message.charAt(0)))
        {
            message = "w" + message;
        }
        int limit = message.indexOf(' ');
        String desc = message;
        String str = "";
        if (limit >= 0)
        {
            desc = message.substring(0, limit);
            str = message.substring(limit+1);
        }
        if (previousMessage == null) {
            BaseComponent text = new TextComponent(str);
            text.setStyle(parseStyle(desc));
            return text;
        }
        Style previousStyle = previousMessage.getStyle();
        BaseComponent ret = previousMessage;
        previousMessage.setStyle(switch (desc.charAt(0)) {
            case '?' -> previousStyle.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, message.substring(1)));
            case '!' -> previousStyle.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, message.substring(1)));
            case '^' -> previousStyle.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, c(message.substring(1))));
            case '@' -> previousStyle.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, message.substring(1)));
            case '&' -> previousStyle.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, message.substring(1)));
            default  -> { // Create a new component
                ret = new TextComponent(str);
                ret.setStyle(parseStyle(desc));
                yield previousStyle; // no op for the previous style
            }
        });
        return ret;
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
            source.sendSuccess(Messenger.c(fields),source.getServer() != null && source.getServer().getLevel(Level.OVERWORLD) != null); //OW
    }
    public static void m(Player player, Object ... fields)
    {
        player.sendMessage(Messenger.c(fields), Util.NIL_UUID);
    }

    /*
    composes single line, multicomponent message, and returns as one chat messagge
     */
    public static BaseComponent c(Object ... fields)
    {
        BaseComponent message = new TextComponent("");
        BaseComponent previousComponent = null;
        for (Object o: fields)
        {
            if (o instanceof BaseComponent)
            {
                message.append((BaseComponent)o);
                previousComponent = (BaseComponent)o;
                continue;
            }
            String txt = o.toString();
            BaseComponent comp = getChatComponentFromDesc(txt, previousComponent);
            if (comp != previousComponent) message.append(comp);
            previousComponent = comp;
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




    public static void send(Player player, Collection<BaseComponent> lines)
    {
        lines.forEach(message -> player.sendMessage(message, Util.NIL_UUID));
    }
    public static void send(CommandSourceStack source, Collection<BaseComponent> lines)
    {
        lines.stream().forEachOrdered((s) -> source.sendSuccess(s, false));
    }


    public static void print_server_message(MinecraftServer server, String message)
    {
        if (server == null)
            LOG.error("Message not delivered: "+message);
        server.sendMessage(new TextComponent(message), Util.NIL_UUID);
        BaseComponent txt = c("gi "+message);
        for (Player entityplayer : server.getPlayerList().getPlayers())
        {
            entityplayer.sendMessage(txt, Util.NIL_UUID);
        }
    }
    public static void print_server_message(MinecraftServer server, BaseComponent message)
    {
        if (server == null)
            LOG.error("Message not delivered: "+message.getString());
        server.sendMessage(message, Util.NIL_UUID);
        for (Player entityplayer : server.getPlayerList().getPlayers())
        {
            entityplayer.sendMessage(message, Util.NIL_UUID);
        }
    }
}

