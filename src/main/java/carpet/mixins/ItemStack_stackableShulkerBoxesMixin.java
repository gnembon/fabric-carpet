package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemInstance.class)
public interface ItemStack_stackableShulkerBoxesMixin
{
    @Inject(method = "getMaxStackSize", at = @At("HEAD"), cancellable = true)
    private void getCMMAxStackSize(CallbackInfoReturnable<Integer> cir)
    {
        if (CarpetSettings.shulkerBoxStackSize > 1
                && ((ItemInstance)((Object)this)) instanceof final ItemStack itemStack
                && itemStack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof ShulkerBoxBlock
                && ((ItemStack) ((Object) this)).getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).allItemsCopyStream().findAny().isEmpty()
        ) {
            cir.setReturnValue(CarpetSettings.shulkerBoxStackSize);
        }
    }
}
