package carpet.mixins;

import carpet.settings.CarpetSettings;
import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShearsItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShearsItem.class)
public class ShearsItem_missingToolsMixin
{
    @Inject(method = "getMiningSpeed", at = @At("HEAD"), cancellable = true)
    private void getCustomMaterial(ItemStack itemStack_1, BlockState blockState_1, CallbackInfoReturnable<Float> cir)
    {
        if (CarpetSettings.missingTools && (blockState_1.getMaterial() == Material.SPONGE))
        {
            cir.setReturnValue(15.0F);
            cir.cancel();
        }
    }
}
