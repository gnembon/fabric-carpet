package carpet.helpers;

import carpet.utils.Messenger;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.block.MaterialColor;
import net.minecraft.util.DyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.BaseText;
import net.minecraft.text.TranslatableText;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class HopperCounter
{
    public static final Map<DyeColor, HopperCounter> COUNTERS;

    static
    {
        EnumMap<DyeColor, HopperCounter> counterMap = new EnumMap<>(DyeColor.class);
        for (DyeColor color : DyeColor.values())
        {
            counterMap.put(color, new HopperCounter(color));
        }
        COUNTERS = Maps.immutableEnumMap(counterMap);
    }

    public final DyeColor color;
    private final String prettyColour;
    private final Object2LongMap<Item> counter = new Object2LongLinkedOpenHashMap<>();
    private long startTick;
    private long startMillis;
    // private PubSubInfoProvider<Long> pubSubProvider;

    private HopperCounter(DyeColor color)
    {
        this.color = color;
        this.prettyColour = getPrettyColour(color);
    }

    public void add(MinecraftServer server, ItemStack stack)
    {
        if (startTick == 0)
        {
            startTick = server.getWorld(World.OVERWORLD).getTime();  //OW
            startMillis = System.currentTimeMillis();
        }
        Item item = stack.getItem();
        counter.put(item, counter.getLong(item) + stack.getCount());
        // pubSubProvider.publish();
    }

    public void reset(MinecraftServer server)
    {
        counter.clear();
        startTick = server.getWorld(World.OVERWORLD).getTime();  //OW
        startMillis = System.currentTimeMillis();
        // pubSubProvider.publish();
    }

    public static void resetAll(MinecraftServer server)
    {
        for (HopperCounter counter : COUNTERS.values())
        {
            counter.reset(server);
        }
    }

    public static List<BaseText> formatAll(MinecraftServer server, boolean realtime)
    {
        List<BaseText> text = new ArrayList<>();

        for (HopperCounter counter : COUNTERS.values())
        {
            List<BaseText> temp = counter.format(server, realtime, false);
            if (temp.size() > 1)
            {
                if (!text.isEmpty()) text.add(Messenger.s(""));
                text.addAll(temp);
            }
        }
        if (text.isEmpty())
        {
            text.add(Messenger.s("No items have been counted yet."));
        }
        return text;
    }

    public List<BaseText> format(MinecraftServer server, boolean realTime, boolean brief)
    {
        if (counter.isEmpty())
        {
            if (brief)
            {
                return Collections.singletonList(Messenger.c("g ", prettyColour,"w : -, -/h, - min "));
            }
            return Collections.singletonList(Messenger.c("w No items for ", prettyColour, "w  yet"));
        }
        long total = getTotalItems();
        long ticks = Math.max(realTime ? (System.currentTimeMillis() - startMillis) / 50 : server.getWorld(World.OVERWORLD).getTime() - startTick, 1);  //OW
        if (total == 0)
        {
            if (brief)
            {
                return Collections.singletonList(Messenger.c(prettyColour,String.format("c : 0, 0/h, %.1f min ", ticks / (20.0 * 60.0))));
            }
            return Collections.singletonList(Messenger.c("w No items for ", prettyColour, String.format("w  yet (%.2f min.%s)",
                    ticks / (20.0 * 60.0), (realTime ? " - real time" : "")),
                    "nb  [X]", "^g reset", "!/counter " + color.getName() +" reset"));
        }
        if (brief)
        {
            return Collections.singletonList(Messenger.c(prettyColour,String.format("c : %d, %d/h, %.1f min ",
                    total, total * (20 * 60 * 60) / ticks, ticks / (20.0 * 60.0))));
        }
        List<BaseText> items = new ArrayList<>();
        items.add(Messenger.c("w Items for ", prettyColour, String.format("w  (%.2f min.%s), total: %d, (%.1f/h):",
                ticks*1.0/(20*60), (realTime?" - real time":""), total, total*1.0*(20*60*60)/ticks),
                "nb [X]", "^g reset", "!/counter "+color+" reset"
        ));
        items.addAll(counter.object2LongEntrySet().stream().sorted((e, f) -> Long.compare(f.getLongValue(), e.getLongValue())).map(e ->
        {
            BaseText itemName = new TranslatableText(e.getKey().getTranslationKey());
            long count = e.getLongValue();
            return Messenger.c("w - ", itemName, String.format("w : %d, %.1f/h",
                    count,
                    count * (20.0 * 60.0 * 60.0) / ticks));
        }).collect(Collectors.toList()));
        return items;
    }

    public static HopperCounter getCounter(String color)
    {
        try
        {
            DyeColor colorEnum = DyeColor.valueOf(color.toUpperCase(Locale.ROOT));
            return COUNTERS.get(colorEnum);
        }
        catch (IllegalArgumentException e)
        {
            return null;
        }
    }

    public long getTotalItems()
    {
        return counter.values().stream().mapToLong(Long::longValue).sum();
    }

    private static String getPrettyColour(DyeColor colour){
        return getPrettyColour(colour, "");
    }

    private static String getPrettyColour(DyeColor colour, String extra){
        if (MaterialColor.WHITE.equals(colour.getMaterialColor())) {
            return "w " + colour.getName() + extra;
        } else if (MaterialColor.ORANGE.equals(colour.getMaterialColor())) {
            return "#F9801D " + colour.getName();//cos not specific code for orange
        } else if (MaterialColor.MAGENTA.equals(colour.getMaterialColor())) {
            return "m " + colour.getName() + extra;
        } else if (MaterialColor.LIGHT_BLUE.equals(colour.getMaterialColor())) {
            return "t " + colour.getName() + extra;
        } else if (MaterialColor.YELLOW.equals(colour.getMaterialColor())) {
            return "y " + colour.getName() + extra;
        } else if (MaterialColor.LIME.equals(colour.getMaterialColor())) {
            return "l " + colour.getName() + extra;
        } else if (MaterialColor.PINK.equals(colour.getMaterialColor())) {
            return "#F38BAA " + colour.getName() + extra;
        } else if (MaterialColor.GRAY.equals(colour.getMaterialColor())) {
            return "f " + colour.getName() + extra;
        } else if (MaterialColor.LIGHT_GRAY.equals(colour.getMaterialColor())) {
            return "g " + colour.getName() + extra;
        } else if (MaterialColor.CYAN.equals(colour.getMaterialColor())) {
            return "c " + colour.getName() + extra;
        } else if (MaterialColor.PURPLE.equals(colour.getMaterialColor())) {
            return "p " + colour.getName() + extra;
        } else if (MaterialColor.BLUE.equals(colour.getMaterialColor())) {
            return "v " + colour.getName() + extra;
        } else if (MaterialColor.BROWN.equals(colour.getMaterialColor())) {
            return "#835432 " + colour.getName() + extra;
        } else if (MaterialColor.GREEN.equals(colour.getMaterialColor())) {
            return "e " + colour.getName() + extra;
        } else if (MaterialColor.RED.equals(colour.getMaterialColor())) {
            return "r " + colour.getName() + extra;
        } else {//if black
            return "k " + colour.getName() + extra;
        }
    }
}
