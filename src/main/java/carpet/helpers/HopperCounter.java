package carpet.helpers;

import carpet.CarpetServer;
import carpet.fakes.IngredientInterface;
import carpet.fakes.RecipeManagerInterface;
import carpet.utils.WoolTool;
import carpet.utils.Messenger;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.block.AbstractBannerBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.MaterialColor;
import net.minecraft.block.Stainable;
import net.minecraft.item.BlockItem;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.util.DyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.BaseText;
import net.minecraft.text.TranslatableText;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The actual object residing in each hopper counter which makes them count the items and saves them. There is one for each
 * colour in MC.
 */

public class HopperCounter
{
    /**
     * A map of all the {@link HopperCounter} counters.
     */
    public static final Map<DyeColor, HopperCounter> COUNTERS;

    /**
     * The default display colour of each item, which makes them look nicer when printing the counter contents to the chat
     */

    public static final TextColor WHITE = TextColor.fromFormatting(Formatting.WHITE);

    static
    {
        EnumMap<DyeColor, HopperCounter> counterMap = new EnumMap<>(DyeColor.class);
        for (DyeColor color : DyeColor.values())
        {
            counterMap.put(color, new HopperCounter(color));
        }
        COUNTERS = Maps.immutableEnumMap(counterMap);
    }

    /**
     * The counter's colour, determined by the colour of wool it's pointing into
     */
    public final DyeColor color;
    /**
     * The string which is passed into {@link Messenger#m} which makes each counter name be displayed in the colour of
     * that counter.
     */
    private final String prettyColour;
    /**
     * All the items stored within the counter, as a map of {@link Item} mapped to a {@code long} of the amount of items
     * stored thus far of that item type.
     */
    private final Object2LongMap<Item> counter = new Object2LongLinkedOpenHashMap<>();
    /**
     * The starting tick of the counter, used to calculate in-game time. Only initialised when the first item enters the
     * counter
     */
    private long startTick;
    /**
     * The starting millisecond of the counter, used to calculate IRl time. Only initialised when the first item enters
     * the counter
     */
    private long startMillis;
    // private PubSubInfoProvider<Long> pubSubProvider;

    private HopperCounter(DyeColor color)
    {
        startTick = -1;
        this.color = color;
        this.prettyColour = WoolTool.Material2DyeName.getOrDefault(color.getMaterialColor(),"w ") + color.getName();
    }

    /**
     * Method used to add items to the counter. Note that this is when the {@link HopperCounter#startTick} and
     * {@link HopperCounter#startMillis} variables are initialised, so you can place the counters and then start the farm
     * after all the collection is sorted out.
     */
    public void add(MinecraftServer server, ItemStack stack)
    {
        if (startTick < 0)
        {
            startTick = server.getWorld(World.OVERWORLD).getTime();  //OW
            startMillis = System.currentTimeMillis();
        }
        Item item = stack.getItem();
        counter.put(item, counter.getLong(item) + stack.getCount());
        // pubSubProvider.publish();
    }

    /**
     * Resets the counter, clearing its items but keeping the clock running.
     */
    public void reset(MinecraftServer server)
    {
        counter.clear();
        startTick = server.getWorld(World.OVERWORLD).getTime();  //OW
        startMillis = System.currentTimeMillis();
        // pubSubProvider.publish();
    }

    /**
     * Resets all counters, clearing their items.
     * @param fresh Whether or not to start the clocks going immediately or later.
     */
    public static void resetAll(MinecraftServer server, boolean fresh)
    {
        for (HopperCounter counter : COUNTERS.values())
        {
            counter.reset(server);
            if (fresh) counter.startTick = -1;
        }
    }

    /**
     * Prints all the counters to chat, nicely formatted, and you can choose whether to diplay in in game time or IRL time
     */
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

    /**
     * Prints a single counter's contents and timings to chat, with the option to keep it short (so no item breakdown,
     * only rates). Again, realtime displays IRL time as opposed to in game time.
     */
    public List<BaseText> format(MinecraftServer server, boolean realTime, boolean brief)
    {
        long ticks = Math.max(realTime ? (System.currentTimeMillis() - startMillis) / 50 : server.getWorld(World.OVERWORLD).getTime() - startTick, 1);  //OW
        if (startTick < 0 || ticks == 0)
        {
            if (brief)
            {
                return Collections.singletonList(Messenger.c("b"+prettyColour,"w : ","gi -, -/h, - min "));
            }
            return Collections.singletonList(Messenger.c(prettyColour, "w  hasn't started counting yet"));
        }
        long total = getTotalItems();
        if (total == 0)
        {
            if (brief)
            {
                return Collections.singletonList(Messenger.c("b"+prettyColour,"w : ","wb 0","w , ","wb 0","w /h, ", String.format("wb %.1f ", ticks / (20.0 * 60.0)), "w min"));
            }
            return Collections.singletonList(Messenger.c("w No items for ", prettyColour, String.format("w  yet (%.2f min.%s)",
                    ticks / (20.0 * 60.0), (realTime ? " - real time" : "")),
                    "nb  [X]", "^g reset", "!/counter " + color.getName() +" reset"));
        }
        if (brief)
        {
            return Collections.singletonList(Messenger.c("b"+prettyColour,"w : ",
                    "wb "+total,"w , ",
                    "wb "+(total * (20 * 60 * 60) / ticks),"w /h, ",
                    String.format("wb %.1f ", ticks / (20.0 * 60.0)), "w min"
            ));
        }
        List<BaseText> items = new ArrayList<>();
        items.add(Messenger.c("w Items for ", prettyColour,
                "w  (",String.format("wb %.2f", ticks*1.0/(20*60)), "w  min"+(realTime?" - real time":"")+"), ",
                "w total: ", "wb "+total, "w , (",String.format("wb %.1f",total*1.0*(20*60*60)/ticks),"w /h):",
                "nb [X]", "^g reset", "!/counter "+color+" reset"
        ));
        items.addAll(counter.object2LongEntrySet().stream().sorted((e, f) -> Long.compare(f.getLongValue(), e.getLongValue())).map(e ->
        {
            Item item = e.getKey();
            BaseText itemName = new TranslatableText(item.getTranslationKey());
            Style itemStyle = itemName.getStyle();
            TextColor color = guessColor(item);
            itemName.setStyle((color != null) ? itemStyle.withColor(color) : itemStyle.withItalic(true));
            long count = e.getLongValue();
            return Messenger.c("g - ", itemName,
                    "g : ","wb "+count,"g , ",
                    String.format("wb %.1f", count * (20.0 * 60.0 * 60.0) / ticks), "w /h"
            );
        }).collect(Collectors.toList()));
        return items;
    }

    /**
     * Converts a colour to have a low brightness and uniform colour, so when it prints the items in different colours
     * it's not too flashy and bright, but enough that it's not dull to look at.
     */
    public static int appropriateColor(int color)
    {
        if (color == 0) return MaterialColor.WHITE.color;
        int r = (color >> 16 & 255);
        int g = (color >> 8 & 255);
        int b = (color & 255);
        if (r < 70) r = 70;
        if (g < 70) g = 70;
        if (b < 70) b = 70;
        return (r << 16) + (g << 8) + b;
    }

    /**
     * Maps items that don't get a good block to reference for colour, or those that colour is wrong to a number of blocks, so we can get their colours easily with the
     * {@link Block#getDefaultMaterialColor()} method as these items have those same colours.
     */
    private static final ImmutableMap<Item, Block> DEFAULTS = new ImmutableMap.Builder<Item, Block>()
            .put(Items.DANDELION, Blocks.YELLOW_WOOL)
            .put(Items.POPPY, Blocks.RED_WOOL)
            .put(Items.BLUE_ORCHID, Blocks.LIGHT_BLUE_WOOL)
            .put(Items.ALLIUM, Blocks.MAGENTA_WOOL)
            .put(Items.AZURE_BLUET, Blocks.SNOW_BLOCK)
            .put(Items.RED_TULIP, Blocks.RED_WOOL)
            .put(Items.ORANGE_TULIP, Blocks.ORANGE_WOOL)
            .put(Items.WHITE_TULIP, Blocks.SNOW_BLOCK)
            .put(Items.PINK_TULIP, Blocks.PINK_WOOL)
            .put(Items.OXEYE_DAISY, Blocks.SNOW_BLOCK)
            .put(Items.CORNFLOWER, Blocks.BLUE_WOOL)
            .put(Items.WITHER_ROSE, Blocks.BLACK_WOOL)
            .put(Items.LILY_OF_THE_VALLEY, Blocks.WHITE_WOOL)
            .put(Items.BROWN_MUSHROOM, Blocks.BROWN_MUSHROOM_BLOCK)
            .put(Items.RED_MUSHROOM, Blocks.RED_MUSHROOM_BLOCK)
            .put(Items.STICK, Blocks.OAK_PLANKS)
            .put(Items.GOLD_INGOT, Blocks.GOLD_BLOCK)
            .put(Items.IRON_INGOT, Blocks.IRON_BLOCK)
            .put(Items.DIAMOND, Blocks.DIAMOND_BLOCK)
            .put(Items.NETHERITE_INGOT, Blocks.NETHERITE_BLOCK)
            .put(Items.SUNFLOWER, Blocks.YELLOW_WOOL)
            .put(Items.LILAC, Blocks.MAGENTA_WOOL)
            .put(Items.ROSE_BUSH, Blocks.RED_WOOL)
            .put(Items.PEONY, Blocks.PINK_WOOL)
            .put(Items.CARROT, Blocks.ORANGE_WOOL)
            .put(Items.APPLE,Blocks.RED_WOOL)
            .put(Items.WHEAT,Blocks.HAY_BLOCK)
            .put(Items.PORKCHOP, Blocks.PINK_WOOL)
            .put(Items.RABBIT,Blocks.PINK_WOOL)
            .put(Items.CHICKEN,Blocks.WHITE_TERRACOTTA)
            .put(Items.BEEF,Blocks.NETHERRACK)
            .put(Items.ENCHANTED_GOLDEN_APPLE,Blocks.GOLD_BLOCK)
            .put(Items.COD,Blocks.WHITE_TERRACOTTA)
            .put(Items.SALMON,Blocks.ACACIA_PLANKS)
            .put(Items.ROTTEN_FLESH,Blocks.BROWN_WOOL)
            .put(Items.PUFFERFISH,Blocks.YELLOW_TERRACOTTA)
            .put(Items.TROPICAL_FISH,Blocks.ORANGE_WOOL)
            .put(Items.POTATO,Blocks.WHITE_TERRACOTTA)
            .put(Items.MUTTON, Blocks.RED_WOOL)
            .put(Items.BEETROOT,Blocks.NETHERRACK)
            .put(Items.MELON_SLICE,Blocks.MELON)
            .put(Items.POISONOUS_POTATO,Blocks.SLIME_BLOCK)
            .put(Items.SPIDER_EYE,Blocks.NETHERRACK)
            .put(Items.GUNPOWDER,Blocks.GRAY_WOOL)
            .put(Items.SCUTE,Blocks.LIME_WOOL)
            .put(Items.FEATHER,Blocks.WHITE_WOOL)
            .put(Items.FLINT,Blocks.BLACK_WOOL)
            .put(Items.LEATHER,Blocks.SPRUCE_PLANKS)
            .put(Items.GLOWSTONE_DUST,Blocks.GLOWSTONE)
            .put(Items.PAPER,Blocks.WHITE_WOOL)
            .put(Items.BRICK,Blocks.BRICKS)
            .put(Items.INK_SAC,Blocks.BLACK_WOOL)
            .put(Items.SNOWBALL,Blocks.SNOW_BLOCK)
            .put(Items.WATER_BUCKET,Blocks.WATER)
            .put(Items.LAVA_BUCKET,Blocks.LAVA)
            .put(Items.MILK_BUCKET,Blocks.WHITE_WOOL)
            .put(Items.CLAY_BALL, Blocks.CLAY)
            .put(Items.COCOA_BEANS,Blocks.COCOA)
            .put(Items.BONE,Blocks.BONE_BLOCK)
            .put(Items.COD_BUCKET,Blocks.BROWN_TERRACOTTA)
            .put(Items.PUFFERFISH_BUCKET,Blocks.YELLOW_TERRACOTTA)
            .put(Items.SALMON_BUCKET,Blocks.PINK_TERRACOTTA)
            .put(Items.TROPICAL_FISH_BUCKET,Blocks.ORANGE_TERRACOTTA)
            .put(Items.SUGAR,Blocks.WHITE_WOOL)
            .put(Items.BLAZE_POWDER,Blocks.GOLD_BLOCK)
            .put(Items.ENDER_PEARL,Blocks.WARPED_PLANKS)
            .put(Items.NETHER_STAR,Blocks.DIAMOND_BLOCK)
            .put(Items.PRISMARINE_CRYSTALS,Blocks.SEA_LANTERN)
            .put(Items.PRISMARINE_SHARD,Blocks.PRISMARINE)
            .put(Items.RABBIT_HIDE,Blocks.OAK_PLANKS)
            .put(Items.CHORUS_FRUIT,Blocks.PURPUR_BLOCK)
            .put(Items.SHULKER_SHELL,Blocks.SHULKER_BOX)
            .put(Items.NAUTILUS_SHELL,Blocks.BONE_BLOCK)
            .put(Items.HEART_OF_THE_SEA,Blocks.CONDUIT)
            .put(Items.HONEYCOMB,Blocks.HONEYCOMB_BLOCK)
            .put(Items.NAME_TAG,Blocks.BONE_BLOCK)
            .put(Items.TOTEM_OF_UNDYING,Blocks.YELLOW_TERRACOTTA)
            .put(Items.TRIDENT,Blocks.PRISMARINE)
            .put(Items.GHAST_TEAR,Blocks.WHITE_WOOL)
            .put(Items.PHANTOM_MEMBRANE,Blocks.BONE_BLOCK)
            .put(Items.EGG,Blocks.BONE_BLOCK)
            //.put(Items.,Blocks.)
            .build();

    /**
     * Gets the colour to print an item in when printing its count in a hopper counter.
     */
    public static TextColor fromItem(Item item)
    {
        if (DEFAULTS.containsKey(item)) return TextColor.fromRgb(appropriateColor(DEFAULTS.get(item).getDefaultMaterialColor().color));
        if (item instanceof DyeItem) return TextColor.fromRgb(appropriateColor(((DyeItem) item).getColor().getMaterialColor().color));
        Block block = null;
        Identifier id = Registry.ITEM.getId(item);
        if (item instanceof BlockItem)
        {
            block = ((BlockItem) item).getBlock();
        }
        else if (Registry.BLOCK.getOrEmpty(id).isPresent())
        {
            block = Registry.BLOCK.get(id);
        }
        if (block != null)
        {
            if (block instanceof AbstractBannerBlock) return TextColor.fromRgb(appropriateColor(((AbstractBannerBlock) block).getColor().getMaterialColor().color));
            if (block instanceof Stainable) return TextColor.fromRgb(appropriateColor( ((Stainable) block).getColor().getMaterialColor().color));
            return TextColor.fromRgb(appropriateColor( block.getDefaultMaterialColor().color));
        }
        return null;
    }

    /**
     * Guesses the item's colour from the item itself. It first calls {@link HopperCounter#fromItem} to see if it has a
     * valid colour there, if not just makes a guess, and if that fails just returns null
     */
    public static TextColor guessColor(Item item)
    {
        TextColor direct = fromItem(item);
        if (direct != null) return direct;
        if (CarpetServer.minecraft_server == null) return WHITE;
        Identifier id = Registry.ITEM.getId(item);
        for (RecipeType<?> type: Registry.RECIPE_TYPE)
        {
            for (Recipe<?> r: ((RecipeManagerInterface) CarpetServer.minecraft_server.getRecipeManager()).getAllMatching(type, id))
            {
                for (Ingredient ingredient: r.getPreviewInputs())
                {
                    for (Collection<ItemStack> stacks : ((IngredientInterface) (Object) ingredient).getRecipeStacks())
                    {
                        for (ItemStack iStak : stacks)
                        {
                            TextColor cand = fromItem(iStak.getItem());
                            if (cand != null)
                                return cand;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the hopper counter from the colour name, if not null
     */
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

    /**
     * The total number of items in the counter
     */
    public long getTotalItems()
    {
        return counter.isEmpty()?0:counter.values().stream().mapToLong(Long::longValue).sum();
    }
}
