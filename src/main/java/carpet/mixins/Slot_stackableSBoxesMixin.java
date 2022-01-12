package carpet.mixins;

import carpet.CarpetSettings;
import carpet.helpers.InventoryHelper;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Slot.class)
public class Slot_stackableSBoxesMixin {
    @Redirect(method = "insertStack(Lnet/minecraft/item/ItemStack;I)Lnet/minecraft/item/ItemStack;", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/screen/slot/Slot;getMaxItemCount(Lnet/minecraft/item/ItemStack;)I"
    ))
    private int getMaxCountForSboxesInSlot(Slot slot, ItemStack stack)
    {
        if (CarpetSettings.shulkerBoxStackSize > 1 &&
                stack.getItem() instanceof BlockItem &&
                ((BlockItem)stack.getItem()).getBlock() instanceof ShulkerBoxBlock &&
                !InventoryHelper.shulkerBoxHasItems(stack)
        )
        {
            return CarpetSettings.shulkerBoxStackSize;
        }
        return slot.getMaxItemCount(stack);
    }
}
