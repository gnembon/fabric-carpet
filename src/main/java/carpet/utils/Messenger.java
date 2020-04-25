package carpet.utils;

import net.minecraft.class_5251;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.entity.EntityCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.BaseText;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.Formatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.dimension.DimensionType;
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
    public static final Logger LOG = LogManager.getLogger();

    private static final Pattern colorExtract = Pattern.compile("#([0-9a-fA-F]{6})");
    public enum CarpetFormatting
    {
        ITALIC      ('i', (s, f) -> s.setItalic(true)),
        STRIKE      ('s', (s, f) -> s.method_27705(Formatting.STRIKETHROUGH)),
        UNDERLINE   ('u', (s, f) -> s.method_27705(Formatting.UNDERLINE)),
        BOLD        ('b', (s, f) -> s.setBold(true)),
        OBFUSCATE   ('o', (s, f) -> s.method_27705(Formatting.OBFUSCATED)),

        WHITE       ('w', (s, f) -> s.setColor(Formatting.WHITE)),
        YELLOW      ('y', (s, f) -> s.setColor(Formatting.YELLOW)),
        LIGHT_PURPLE('m', (s, f) -> s.setColor(Formatting.LIGHT_PURPLE)), // magenta
        RED         ('r', (s, f) -> s.setColor(Formatting.RED)),
        AQUA        ('c', (s, f) -> s.setColor(Formatting.AQUA)), // cyan
        GREEN       ('l', (s, f) -> s.setColor(Formatting.GREEN)), // lime
        BLUE        ('t', (s, f) -> s.setColor(Formatting.BLUE)), // light blue, teal
        DARK_GRAY   ('f', (s, f) -> s.setColor(Formatting.DARK_GRAY)),
        GRAY        ('g', (s, f) -> s.setColor(Formatting.GRAY)),
        GOLD        ('d', (s, f) -> s.setColor(Formatting.GOLD)),
        DARK_PURPLE ('p', (s, f) -> s.setColor(Formatting.DARK_PURPLE)), // purple
        DARK_RED    ('n', (s, f) -> s.setColor(Formatting.DARK_RED)),  // brown
        DARK_AQUA   ('q', (s, f) -> s.setColor(Formatting.DARK_AQUA)),
        DARK_GREEN  ('e', (s, f) -> s.setColor(Formatting.DARK_GREEN)),
        DARK_BLUE   ('v', (s, f) -> s.setColor(Formatting.DARK_BLUE)), // navy
        BLACK       ('k', (s, f) -> s.setColor(Formatting.BLACK)),

        COLOR       ('#', (s, f) -> {
            class_5251 color = class_5251.method_27719("#"+f);
            return color == null ? s : s.method_27703(color);
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

    private static BaseText _applyStyleToTextComponent(BaseText comp, String style)
    {
        Style myStyle= Style.field_24360.setColor(Formatting.WHITE);
        for (CarpetFormatting cf: CarpetFormatting.values())
        {
            myStyle = cf.apply(style, myStyle);
        }
        comp.setStyle(myStyle);
        return comp;
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
    public static String creatureTypeColor(EntityCategory type)
    {
        switch (type)
        {
            case MONSTER:
                return "n";
            case CREATURE:
                return "e";
            case AMBIENT:
                return "f";
            case WATER_CREATURE:
                return "v";
        }
        return "w";
    }

    private static BaseText _getChatComponentFromDesc(String message, BaseText previous_message)
    {
        if (message.equalsIgnoreCase(""))
        {
            return new LiteralText("");
        }
        if (Character.isWhitespace(message.charAt(0)))
        {
            message = "w"+message;
        }
        String[] parts = message.split("\\s", 2);
        String desc = parts[0];
        String str = "";
        if (parts.length > 1) str = parts[1];
        if (desc.charAt(0) == '/') // deprecated
        {
            if (previous_message != null)
                previous_message.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, message));
            return previous_message;
        }
        if (desc.charAt(0) == '?')
        {
            if (previous_message != null)
                previous_message.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, message.substring(1)));
            return previous_message;
        }
        if (desc.charAt(0) == '!')
        {
            if (previous_message != null)
                previous_message.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, message.substring(1)));
            return previous_message;
        }
        if (desc.charAt(0) == '^')
        {
            if (previous_message != null)
                previous_message.getStyle().setHoverEvent(new HoverEvent(HoverEvent.class_5247.field_24342, c(message.substring(1))));
            return previous_message;
        }
        BaseText txt = new LiteralText(str);
        return _applyStyleToTextComponent(txt, desc);
    }
    public static BaseText tp(String desc, Vec3d pos) { return tp(desc, pos.x, pos.y, pos.z); }
    public static BaseText tp(String desc, BlockPos pos) { return tp(desc, pos.getX(), pos.getY(), pos.getZ()); }
    public static BaseText tp(String desc, double x, double y, double z) { return tp(desc, (float)x, (float)y, (float)z);}
    public static BaseText tp(String desc, float x, float y, float z)
    {
        return _getCoordsTextComponent(desc, x, y, z, false);
    }
    public static BaseText tp(String desc, int x, int y, int z)
    {
        return _getCoordsTextComponent(desc, (float)x, (float)y, (float)z, true);
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

    private static BaseText _getCoordsTextComponent(String style, float x, float y, float z, boolean isInt)
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
        source.sendFeedback(Messenger.c(fields),source.getMinecraftServer().getWorld(DimensionType.OVERWORLD) != null);
    }
    public static void m(PlayerEntity player, Object ... fields)
    {
        player.sendSystemMessage(Messenger.c(fields));
    }

    /*
    composes single line, multicomponent message, and returns as one chat messagge
     */
    public static BaseText c(Object ... fields)
    {
        BaseText message = new LiteralText("");
        BaseText previous_component = null;
        for (Object o: fields)
        {
            if (o instanceof BaseText)
            {
                message.append((BaseText)o);
                previous_component = (BaseText)o;
                continue;
            }
            String txt = o.toString();
            BaseText comp = _getChatComponentFromDesc(txt,previous_component);
            if (comp != previous_component) message.append(comp);
            previous_component = comp;
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
        _applyStyleToTextComponent(message, style);
        return message;
    }




    public static void send(PlayerEntity player, Collection<BaseText> lines)
    {
        lines.forEach(player::sendSystemMessage);
    }
    public static void send(ServerCommandSource source, Collection<BaseText> lines)
    {
        lines.stream().forEachOrdered((s) -> source.sendFeedback(s, false));
    }


    public static void print_server_message(MinecraftServer server, String message)
    {
        if (server == null)
            LOG.error("Message not delivered: "+message);
        server.sendSystemMessage(new LiteralText(message));
        BaseText txt = c("gi "+message);
        for (PlayerEntity entityplayer : server.getPlayerManager().getPlayerList())
        {
            entityplayer.sendSystemMessage(txt);
        }
    }
    public static void print_server_message(MinecraftServer server, BaseText message)
    {
        if (server == null)
            LOG.error("Message not delivered: "+message.getString());
        server.sendSystemMessage(message);
        for (PlayerEntity entityplayer : server.getPlayerManager().getPlayerList())
        {
            entityplayer.sendSystemMessage(message);
        }
    }
}

