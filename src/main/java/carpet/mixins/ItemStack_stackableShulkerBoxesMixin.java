package carpet.mixins;

import carpet.CarpetSettings;
import carpet.helpers.ShulkerHelper;
import net.minecraft.world.item.ItemStack;
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
        if (CarpetSettings.shulkerBoxStackSize > 1 && ShulkerHelper.isEmptyBox((ItemStack)(Object)this))
        {
            cir.setReturnValue(CarpetSettings.shulkerBoxStackSize);
        }
    }
}
