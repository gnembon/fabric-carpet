package carpet.utils;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
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
            TextColor color;
            try
            {
                color = TextColor.parseColor("#" + f).getOrThrow(RuntimeException::new);
            }
            catch (RuntimeException e)
            {
                return s;
            }
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

    private static MutableComponent getChatComponentFromDesc(String message, MutableComponent previousMessage)
    {
        if (message.equalsIgnoreCase(""))
        {
            return Component.literal("");
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
            MutableComponent text = Component.literal(str);
            text.setStyle(parseStyle(desc));
            return text;
        }
        Style previousStyle = previousMessage.getStyle();
        MutableComponent ret = previousMessage;
        previousMessage.setStyle(switch (desc.charAt(0)) {
            case '?' -> previousStyle.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, message.substring(1)));
            case '!' -> previousStyle.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, message.substring(1)));
            case '^' -> previousStyle.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, c(message.substring(1))));
            case '@' -> previousStyle.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, message.substring(1)));
            case '&' -> previousStyle.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, message.substring(1)));
            default  -> { // Create a new component
                ret = Component.literal(str);
                ret.setStyle(parseStyle(desc));
                yield previousStyle; // no op for the previous style
            }
        });
        return ret;
    }
    public static Component tp(String desc, Vec3 pos) { return tp(desc, pos.x, pos.y, pos.z); }
    public static Component tp(String desc, BlockPos pos) { return tp(desc, pos.getX(), pos.getY(), pos.getZ()); }
    public static Component tp(String desc, double x, double y, double z) { return tp(desc, (float)x, (float)y, (float)z);}
    public static Component tp(String desc, float x, float y, float z)
    {
        return getCoordsTextComponent(desc, x, y, z, false);
    }
    public static Component tp(String desc, int x, int y, int z)
    {
        return getCoordsTextComponent(desc, x, y, z, true);
    }

    /// to be continued
    public static Component dbl(String style, double double_value)
    {
        return c(String.format("%s %.1f",style,double_value),String.format("^w %f",double_value));
    }
    public static Component dbls(String style, double ... doubles)
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
    public static Component dblf(String style, double ... doubles)
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
    public static Component dblt(String style, double ... doubles)
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

    private static Component getCoordsTextComponent(String style, float x, float y, float z, boolean isInt)
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
            source.sendSuccess(() -> Messenger.c(fields), source.getServer() != null && source.getServer().overworld() != null);
    }
    public static void m(Player player, Object ... fields)
    {
        ((ServerPlayer)player).sendSystemMessage(Messenger.c(fields));
    }

    /*
    composes single line, multicomponent message, and returns as one chat messagge
     */
    public static Component c(Object ... fields)
    {
        MutableComponent message = Component.literal("");
        MutableComponent previousComponent = null;
        for (Object o: fields)
        {
            if (o instanceof MutableComponent)
            {
                message.append((MutableComponent)o);
                previousComponent = (MutableComponent)o;
                continue;
            }
            String txt = o.toString();
            MutableComponent comp = getChatComponentFromDesc(txt, previousComponent);
            if (comp != previousComponent) message.append(comp);
            previousComponent = comp;
        }
        return message;
    }

    //simple text

    public static Component s(String text)
    {
        return s(text,"");
    }
    public static Component s(String text, String style)
    {
        MutableComponent message = Component.literal(text);
        message.setStyle(parseStyle(style));
        return message;
    }




    public static void send(Player player, Collection<Component> lines)
    {
        lines.forEach(message -> ((ServerPlayer)player).sendSystemMessage(message));
    }
    public static void send(CommandSourceStack source, Collection<Component> lines)
    {
        lines.stream().forEachOrdered((s) -> source.sendSuccess(() -> s, false));
    }


    public static void print_server_message(MinecraftServer server, String message)
    {
        if (server == null)
            LOG.error("Message not delivered: "+message);
        server.sendSystemMessage(Component.literal(message));
        Component txt = c("gi "+message);
        for (ServerPlayer entityplayer : server.getPlayerList().getPlayers())
        {
            entityplayer.sendSystemMessage(txt);
        }
    }
    public static void print_server_message(MinecraftServer server, Component message)
    {
        if (server == null)
            LOG.error("Message not delivered: "+message.getString());
        server.sendSystemMessage(message);
        for (ServerPlayer entityplayer : server.getPlayerList().getPlayers())
        {
            entityplayer.sendSystemMessage(message);
        }
    }
}

