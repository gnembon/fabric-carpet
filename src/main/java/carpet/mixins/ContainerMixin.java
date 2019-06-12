package carpet.mixins;

import carpet.settings.CarpetSettings;
import net.minecraft.container.Container;
import net.minecraft.container.Slot;
import net.minecraft.container.SlotActionType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Container.class)
public class ContainerMixin
{
    @Shadow @Final public List<Slot> slotList;

    @Inject( method = "onSlotClick", at = @At(value = "HEAD"), cancellable = true)
    private void onThrowClick(
            int int_1,
            int int_2,
            SlotActionType slotActionType_1,
            PlayerEntity playerEntity_1,
            CallbackInfoReturnable<ItemStack> cir
    )
    {
        if (slotActionType_1 == SlotActionType.THROW && CarpetSettings.ctrlQCraftingFix && playerEntity_1.inventory.getCursorStack().isEmpty() && int_1 >= 0)
        {
            ItemStack itemStack_1 = ItemStack.EMPTY;
            Slot slot_4 = slotList.get(int_1);
            if (slot_4 != null && slot_4.canTakeItems(playerEntity_1))
            {
                if(int_1 == 0 && int_2 == 1)
                {
                    ItemStack itemStackDropAll = dropAllCrafting(playerEntity_1, int_1, slotList);
                    while (!itemStackDropAll.isEmpty() && ItemStack.areItemsEqual(slot_4.getStack(), itemStackDropAll))
                    {
                        itemStack_1 = itemStackDropAll.copy();
                        itemStackDropAll = dropAllCrafting(playerEntity_1, int_1, slotList);
                    }
                    cir.setReturnValue(itemStack_1);
                    cir.cancel();
                }
            }
        }
    }

    private ItemStack dropAllCrafting(PlayerEntity playerIn, int index, List<Slot> invSlotParam)
    {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = invSlotParam.get(index);
        if (slot != null && slot.hasStack())
        {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            if (index == 0)
            {
                playerIn.dropItem(itemstack, true);
                itemstack1.setCount(0);
                slot.onStackChanged(itemstack1, itemstack);
            }
            if (itemstack.getCount() == itemstack1.getCount())
            {
                return ItemStack.EMPTY;
            }
            ItemStack itemstack2 = slot.onTakeItem(playerIn, itemstack1);
            if (index == 0)
            {
                playerIn.dropItem(itemstack2, false);
            }
        }
        return itemstack;
    }
}
