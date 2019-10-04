package carpet.mixins;

import carpet.settings.CarpetSettings;
import net.minecraft.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.Settings.class)
public class __Block_honeyMixin
{
    @Shadow private float slipperiness;

    @Shadow private float hardness;

    @Shadow private float resistance;

    @Inject(method = "slipperiness", at = @At("HEAD"), cancellable = true)
    private void setSlipperiness(float float_1, CallbackInfoReturnable<Block.Settings> cir)
    {
        if (float_1==0.8f)
        {
            slipperiness = 0.8f;
            hardness = 0.10f;
            resistance = 0.10f;
            cir.setReturnValue((Block.Settings) (Object)this);
        }
    }
}
