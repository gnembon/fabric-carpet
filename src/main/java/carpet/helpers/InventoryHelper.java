package carpet.helpers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import static net.minecraft.nbt.Tag.*;

public class InventoryHelper
{
    public static boolean cleanUpShulkerBoxTag(ItemStack stack)
    {
        boolean changed = false;
        CompoundTag tag = stack.getTag();

        if (tag == null || !tag.contains("BlockEntityTag", TAG_COMPOUND))
            return false;

        CompoundTag bet = tag.getCompound("BlockEntityTag");
        if (bet.contains("Items", TAG_LIST) && bet.getList("Items", TAG_COMPOUND).isEmpty())
        {
            bet.remove("Items");
            changed = true;
        }

        if (bet.isEmpty() || (bet.size() == 1 && bet.getString("id").equals("minecraft:shulker_box")))
        {
            tag.remove("BlockEntityTag");
            changed = true;
        }
        if (tag.isEmpty())
        {
            stack.setTag(null);
            changed = true;
        }
        return changed;
    }

    public static boolean shulkerBoxHasItems(ItemStack stack)
    {
        CompoundTag tag = stack.getTag();

        if (tag == null || !tag.contains("BlockEntityTag", TAG_COMPOUND))
            return false;

        CompoundTag bet = tag.getCompound("BlockEntityTag");
        return bet.contains("Items", TAG_LIST) && !bet.getList("Items", TAG_COMPOUND).isEmpty();
    }
}
