package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.world.level.block.SculkSensorBlock;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SculkSensorBlock.class)
public class SculkSensorBlock_rangeMixin {

    @Shadow
    @Final
    private int listenerRange;


    @Inject(
            method = "getListenerRange()I",
            at = @At("HEAD"),
            cancellable = true
    )
    public void getListenerRange(CallbackInfoReturnable<Integer> cir) {
        if (CarpetSettings.sculkSensorRange != this.listenerRange) {
            cir.setReturnValue(CarpetSettings.sculkSensorRange);
        }
    }
}
