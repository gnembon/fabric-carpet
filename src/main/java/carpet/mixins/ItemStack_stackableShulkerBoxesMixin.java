package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStack_stackableShulkerBoxesMixin
{
    @Inject(method = "getMaxStackSize", at = @At("HEAD"), cancellable = true)
    private void getCMMAxStackSize(CallbackInfoReturnable<Integer> cir)
    {
        if (CarpetSettings.shulkerBoxStackSize > 1
                && ((ItemStack)((Object)this)).getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof ShulkerBoxBlock
                && ((ItemStack) ((Object) this)).getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).stream().findAny().isEmpty()
        ) {
            cir.setReturnValue(CarpetSettings.shulkerBoxStackSize);
        }
    }
}
