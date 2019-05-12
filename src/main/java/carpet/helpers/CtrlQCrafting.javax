package carpet.helpers;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import java.util.List;

public class CtrlQCrafting {

    public static ItemStack dropAllCrafting(EntityPlayer playerIn, int index, List<Slot> invSlotParam)
    {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = invSlotParam.get(index);

        if (slot != null && slot.getHasStack())
        {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            EntityEquipmentSlot entityequipmentslot = EntityLiving.getSlotForItemStack(itemstack);

            if (index == 0)
            {
                playerIn.dropItem(itemstack, true);

                itemstack1.setCount(0);

                slot.onSlotChange(itemstack1, itemstack);
            }

            if (itemstack.getCount() == itemstack1.getCount())
            {
                return ItemStack.EMPTY;
            }

            ItemStack itemstack2 = slot.onTake(playerIn, itemstack1);

            if (index == 0)
            {
                playerIn.dropItem(itemstack2, false);
            }
        }

        return itemstack;
    }
}
