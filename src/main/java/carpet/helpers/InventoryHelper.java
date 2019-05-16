package carpet.helpers;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

public class InventoryHelper
{
    // From nbt/Tag.java createTag()
    public static final int TAG_END         = 0;
    public static final int TAG_BYTE        = 1;
    public static final int TAG_SHORT       = 2;
    public static final int TAG_INT         = 3;
    public static final int TAG_LONG        = 4;
    public static final int TAG_FLOAT       = 5;
    public static final int TAG_DOUBLE      = 6;
    public static final int TAG_BYTEARRAY   = 7;
    public static final int TAG_STRING      = 8;
    public static final int TAG_LIST        = 9;
    public static final int TAG_COMPOUND    = 10;
    public static final int TAG_INTARRAY    = 11;
    public static final int TAG_LONGARRAY   = 12;

    public static boolean cleanUpShulkerBoxTag(ItemStack stack)
    {
        boolean changed = false;
        CompoundTag tag = stack.getTag();

        if (tag == null || !tag.containsKey("BlockEntityTag", TAG_COMPOUND))
            return false;

        CompoundTag bet = tag.getCompound("BlockEntityTag");
        if (bet.containsKey("Items", TAG_LIST) && bet.getList("Items", TAG_COMPOUND).isEmpty())
        {
            bet.remove("Items");
            changed = true;
        }

        if (bet.isEmpty())
        {
            stack.setTag(null);
            changed = true;
        }

        return changed;
    }

    public static boolean shulkerBoxHasItems(ItemStack stack)
    {
        CompoundTag tag = stack.getTag();

        if (tag == null || !tag.containsKey("BlockEntityTag", TAG_COMPOUND))
            return false;

        CompoundTag bet = tag.getCompound("BlockEntityTag");
        return bet.containsKey("Items", TAG_LIST) && !bet.getList("Items", TAG_COMPOUND).isEmpty();
    }
}
