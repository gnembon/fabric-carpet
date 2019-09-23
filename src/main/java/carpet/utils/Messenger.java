package carpet.utils;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.entity.EntityCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.text.BaseText;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.world.dimension.DimensionType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Messenger
{
    public static final Logger LOG = LogManager.getLogger();

    /*
     messsage: "desc me ssa ge"
     desc contains:
     i = italic
     s = strikethrough
     u = underline
     b = bold
     o = obfuscated

     w = white
     y = yellow
     m = magenta (light purple)
     r = red
     c = cyan (aqua)
     l = lime (green)
     t = light blue (blue)
     f = dark gray
     g = gray
     d = gold
     p = dark purple (purple)
     n = dark red (brown)
     q = dark aqua
     e = dark green
     v = dark blue (navy)
     k = black

     / = action added to the previous component
     */

    private static BaseText _applyStyleToTextComponent(BaseText comp, String style)
    {
        //could be rewritten to be more efficient
        comp.getStyle().setItalic(style.indexOf('i')>=0);
        comp.getStyle().setStrikethrough(style.indexOf('s')>=0);
        comp.getStyle().setUnderline(style.indexOf('u')>=0);
        comp.getStyle().setBold(style.indexOf('b')>=0);
        comp.getStyle().setObfuscated(style.indexOf('o')>=0);
        comp.getStyle().setColor(Formatting.WHITE);
        if (style.indexOf('w')>=0) comp.getStyle().setColor(Formatting.WHITE); // not needed
        if (style.indexOf('y')>=0) comp.getStyle().setColor(Formatting.YELLOW);
        if (style.indexOf('m')>=0) comp.getStyle().setColor(Formatting.LIGHT_PURPLE);
        if (style.indexOf('r')>=0) comp.getStyle().setColor(Formatting.RED);
        if (style.indexOf('c')>=0) comp.getStyle().setColor(Formatting.AQUA);
        if (style.indexOf('l')>=0) comp.getStyle().setColor(Formatting.GREEN);
        if (style.indexOf('t')>=0) comp.getStyle().setColor(Formatting.BLUE);
        if (style.indexOf('f')>=0) comp.getStyle().setColor(Formatting.DARK_GRAY);
        if (style.indexOf('g')>=0) comp.getStyle().setColor(Formatting.GRAY);
        if (style.indexOf('d')>=0) comp.getStyle().setColor(Formatting.GOLD);
        if (style.indexOf('p')>=0) comp.getStyle().setColor(Formatting.DARK_PURPLE);
        if (style.indexOf('n')>=0) comp.getStyle().setColor(Formatting.DARK_RED);
        if (style.indexOf('q')>=0) comp.getStyle().setColor(Formatting.DARK_AQUA);
        if (style.indexOf('e')>=0) comp.getStyle().setColor(Formatting.DARK_GREEN);
        if (style.indexOf('v')>=0) comp.getStyle().setColor(Formatting.DARK_BLUE);
        if (style.indexOf('k')>=0) comp.getStyle().setColor(Formatting.BLACK);
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
        String parts[] = message.split("\\s", 2);
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
                previous_message.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, c(message.substring(1))));
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
        player.sendMessage(Messenger.c(fields));
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
        lines.forEach(player::sendMessage);
    }
    public static void send(ServerCommandSource source, Collection<BaseText> lines)
    {
        lines.stream().forEachOrdered((s) -> source.sendFeedback(s, false));
    }


    public static void print_server_message(MinecraftServer server, String message)
    {
        if (server == null)
            LOG.error("Message not delivered: "+message);
        server.sendMessage(new LiteralText(message));
        BaseText txt = c("gi "+message);
        for (PlayerEntity entityplayer : server.getPlayerManager().getPlayerList())
        {
            entityplayer.sendMessage(txt);
        }
    }
    public static void print_server_message(MinecraftServer server, BaseText message)
    {
        if (server == null)
            LOG.error("Message not delivered: "+message.getString());
        server.sendMessage(message);
        for (PlayerEntity entityplayer : server.getPlayerManager().getPlayerList())
        {
            entityplayer.sendMessage(message);
        }
    }
}

