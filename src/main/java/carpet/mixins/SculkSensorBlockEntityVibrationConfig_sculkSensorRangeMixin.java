package carpet.mixins;


import carpet.CarpetSettings;
import net.minecraft.world.level.block.entity.SculkSensorBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SculkSensorBlockEntity.VibrationUser.class)
public class SculkSensorBlockEntityVibrationConfig_sculkSensorRangeMixin
{
    @Inject(method = "getListenerRadius", at = @At("HEAD"), cancellable = true)
    private void sculkSensorRange(CallbackInfoReturnable<Integer> cir)
    {
        if (CarpetSettings.sculkSensorRange != SculkSensorBlockEntity.VibrationUser.LISTENER_RANGE) {
            cir.setReturnValue(CarpetSettings.sculkSensorRange);
        }
    }
}
