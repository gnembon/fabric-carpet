package carpet.utils;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.BaseText;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.Formatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
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
        STRIKE      ('s', (s, f) -> s.withFormatting(Formatting.STRIKETHROUGH)),
        UNDERLINE   ('u', (s, f) -> s.withFormatting(Formatting.UNDERLINE)),
        BOLD        ('b', (s, f) -> s.withBold(true)),
        OBFUSCATE   ('o', (s, f) -> s.withFormatting(Formatting.OBFUSCATED)),

        WHITE       ('w', (s, f) -> s.withColor(Formatting.WHITE)),
        YELLOW      ('y', (s, f) -> s.withColor(Formatting.YELLOW)),
        LIGHT_PURPLE('m', (s, f) -> s.withColor(Formatting.LIGHT_PURPLE)), // magenta
        RED         ('r', (s, f) -> s.withColor(Formatting.RED)),
        AQUA        ('c', (s, f) -> s.withColor(Formatting.AQUA)), // cyan
        GREEN       ('l', (s, f) -> s.withColor(Formatting.GREEN)), // lime
        BLUE        ('t', (s, f) -> s.withColor(Formatting.BLUE)), // light blue, teal
        DARK_GRAY   ('f', (s, f) -> s.withColor(Formatting.DARK_GRAY)),
        GRAY        ('g', (s, f) -> s.withColor(Formatting.GRAY)),
        GOLD        ('d', (s, f) -> s.withColor(Formatting.GOLD)),
        DARK_PURPLE ('p', (s, f) -> s.withColor(Formatting.DARK_PURPLE)), // purple
        DARK_RED    ('n', (s, f) -> s.withColor(Formatting.DARK_RED)),  // brown
        DARK_AQUA   ('q', (s, f) -> s.withColor(Formatting.DARK_AQUA)),
        DARK_GREEN  ('e', (s, f) -> s.withColor(Formatting.DARK_GREEN)),
        DARK_BLUE   ('v', (s, f) -> s.withColor(Formatting.DARK_BLUE)), // navy
        BLACK       ('k', (s, f) -> s.withColor(Formatting.BLACK)),

        COLOR       ('#', (s, f) -> {
            TextColor color = TextColor.parse("#"+f);
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
        Style myStyle= Style.EMPTY.withColor(Formatting.WHITE);
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
    public static String creatureTypeColor(SpawnGroup type)
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

    private static BaseText getChatComponentFromDesc(String message, BaseText previousMessage)
    {
        if (message.equalsIgnoreCase(""))
        {
            return new LiteralText("");
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
            BaseText text = new LiteralText(str);
            text.setStyle(parseStyle(desc));
            return text;
        }
        Style previousStyle = previousMessage.getStyle();
        BaseText ret = previousMessage;
        previousMessage.setStyle(switch (desc.charAt(0)) {
            case '?' -> previousStyle.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, message.substring(1)));
            case '!' -> previousStyle.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, message.substring(1)));
            case '^' -> previousStyle.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, c(message.substring(1))));
            case '@' -> previousStyle.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, message.substring(1)));
            case '&' -> previousStyle.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, message.substring(1)));
            default  -> { // Create a new component
                ret = new LiteralText(str);
                ret.setStyle(parseStyle(desc));
                yield previousStyle; // no op for the previous style
            }
        });
        return ret;
    }
    public static BaseText tp(String desc, Vec3d pos) { return tp(desc, pos.x, pos.y, pos.z); }
    public static BaseText tp(String desc, BlockPos pos) { return tp(desc, pos.getX(), pos.getY(), pos.getZ()); }
    public static BaseText tp(String desc, double x, double y, double z) { return tp(desc, (float)x, (float)y, (float)z);}
    public static BaseText tp(String desc, float x, float y, float z)
    {
        return getCoordsTextComponent(desc, x, y, z, false);
    }
    public static BaseText tp(String desc, int x, int y, int z)
    {
        return getCoordsTextComponent(desc, (float)x, (float)y, (float)z, true);
    }

    /// to be continued
    public static BaseText dbl(String style, double double_value)
    {
        return c(String.format("%s %.1f",style,double_value),String.format("^w %f",double_value));
    }
    public static BaseText dbls(String style, double ... doubles)
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
    public static BaseText dblf(String style, double ... doubles)
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
    public static BaseText dblt(String style, double ... doubles)
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

    private static BaseText getCoordsTextComponent(String style, float x, float y, float z, boolean isInt)
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
    public static void m(ServerCommandSource source, Object ... fields)
    {
        if (source != null)
            source.sendFeedback(Messenger.c(fields),source.getServer() != null && source.getServer().getWorld(World.OVERWORLD) != null); //OW
    }
    public static void m(PlayerEntity player, Object ... fields)
    {
        player.sendSystemMessage(Messenger.c(fields), Util.NIL_UUID);
    }

    /*
    composes single line, multicomponent message, and returns as one chat messagge
     */
    public static BaseText c(Object ... fields)
    {
        BaseText message = new LiteralText("");
        BaseText previousComponent = null;
        for (Object o: fields)
        {
            if (o instanceof BaseText)
            {
                message.append((BaseText)o);
                previousComponent = (BaseText)o;
                continue;
            }
            String txt = o.toString();
            BaseText comp = getChatComponentFromDesc(txt, previousComponent);
            if (comp != previousComponent) message.append(comp);
            previousComponent = comp;
        }
        return message;
    }

    //simple text

    public static BaseText s(String text)
    {
        return s(text,"");
    }
    public static BaseText s(String text, String style)
    {
        BaseText message = new LiteralText(text);
        message.setStyle(parseStyle(style));
        return message;
    }




    public static void send(PlayerEntity player, Collection<BaseText> lines)
    {
        lines.forEach(message -> player.sendSystemMessage(message, Util.NIL_UUID));
    }
    public static void send(ServerCommandSource source, Collection<BaseText> lines)
    {
        lines.stream().forEachOrdered((s) -> source.sendFeedback(s, false));
    }


    public static void print_server_message(MinecraftServer server, String message)
    {
        if (server == null)
            LOG.error("Message not delivered: "+message);
        server.sendSystemMessage(new LiteralText(message), Util.NIL_UUID);
        BaseText txt = c("gi "+message);
        for (PlayerEntity entityplayer : server.getPlayerManager().getPlayerList())
        {
            entityplayer.sendSystemMessage(txt, Util.NIL_UUID);
        }
    }
    public static void print_server_message(MinecraftServer server, BaseText message)
    {
        if (server == null)
            LOG.error("Message not delivered: "+message.getString());
        server.sendSystemMessage(message, Util.NIL_UUID);
        for (PlayerEntity entityplayer : server.getPlayerManager().getPlayerList())
        {
            entityplayer.sendSystemMessage(message, Util.NIL_UUID);
        }
    }
}

