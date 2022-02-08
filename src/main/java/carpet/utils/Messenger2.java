package carpet.utils;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.BaseComponent;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.KeybindComponent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

import net.minecraft.world.entity.Entity;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;

import java.util.function.BiFunction;

import java.util.regex.Matcher;
import java.util.function.Function;
public class Messenger2 {
    ArrayList<Object> data = new ArrayList<>();
    int c;
    int size;



    public static Style parseStyle(String style)
    {
        Style myStyle= Style.EMPTY.withColor(ChatFormatting.WHITE);
        for (CarpetFormatting cf: CarpetFormatting.values()) myStyle = cf.apply(style, myStyle);
        return myStyle;
    }

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

    Messenger2(Object... data) {
        for (Object x : data) {
            this.data.add(x);
        }
        this.size = this.data.size();
        c = 0;
    }

    /*
     * composes single line, multicomponent message, and returns as one chat
     * messagge
     */

    public Component getSomeComponents() {
        TextComponent message = new TextComponent("");
        while (true) {
            if (isEnd()) {
                return message;
            }
            Component com = getAComponent();
            message.append(com);
        }
    }

    private Component getAComponent() {
        BaseComponent base = (BaseComponent) getARawComponent();
        while (isModifier(now())) {
            base = (BaseComponent) getModifier(((String) now()).charAt(0)).apply(base, (String) now());
        }
        return base;
    }

    private BiFunction<BaseComponent, String, Component> getModifier(char ch) {
        c += 1;
        return (base, message) -> {
            Style previousStyle = base.getStyle();
            base.setStyle(switch (ch) {
                case '?' -> previousStyle
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, message.substring(1)));
                case '!' -> previousStyle
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, message.substring(1)));
                case '^' -> previousStyle
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, getSomeComponents()));
                case '@' -> previousStyle
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, message.substring(1)));
                case '&' -> previousStyle
                        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, message.substring(1)));
                default -> null;
            });
            return base;
        };

    }

    private static boolean isModifier(Object peek) {
        if (peek instanceof String pek) {
            if (pek.startsWith("^") || pek.startsWith("?") || pek.startsWith("!") || pek.startsWith("@")
                    || pek.startsWith("&")) {
                return true;
            }
        }
        return false;
    }

    private Component getARawComponent() {
        Component res;
        if (now() instanceof Component b) {
            res=  b;
            c += 1;return res;
        }
        if (now() instanceof Entity ent) {
            res=  (ent.getDisplayName());
            c += 1;return res;
        }
        if (now() instanceof ItemStack ent) {
            res= (ent.getDisplayName());
            c += 1;return res;
        }
        if (now() instanceof String message) {
            if (message.equalsIgnoreCase("")) {
                return new TextComponent("");
            }
            if (message.charAt(0)==' ') {
                message = "w" + message;
            }
            int limit = message.indexOf(' ');
            String desc = message;
            String str = "";
            if (limit >= 0) {
                desc = message.substring(0, limit);
                str = message.substring(limit + 1);
            }
            c += 1;
            ArrayList<Object> args = new ArrayList<>();
            if(desc.indexOf('j')>=0){
                while(true){
                    if(isEnd())break;
                    args.add(getAComponent());
                }
                
            }
            BaseComponent text = desc.indexOf('j')>=0? new TranslatableComponent(str,args.toArray()):!desc.startsWith("_") ? new TextComponent(str):new KeybindComponent(str);
            text.setStyle(parseStyle(desc));
            
            return text;
        }

        return null;
        // TO DO
    }

    private boolean isEnd() {
        if (c >= size)
            return true;
        if ("|".equals(now())) {
            c += 1;
            return true;
        }
        ;
        return false;
    }



    private Object now() {
        if (c >= size)
            return null;
        return data.get(c);

    }

    public static Component c(Object... fields) {
        Messenger2 m = new Messenger2(fields);

        if (m.size < 1) {
            return new TextComponent("");
        } else {
            return m.getSomeComponents();
        }
    }

    // simple text

}
