package carpet.mixins;

import carpet.settings.CarpetSettings;
import net.minecraft.block.PistonBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonBlock.class)
public class PistonBlock_qcMixin
{
    @Inject(method = "shouldExtend", cancellable = true, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/math/BlockPos;up()Lnet/minecraft/util/math/BlockPos;"
    ))
    private void cancelUpCheck(World world_1, BlockPos blockPos_1, Direction direction_1, CallbackInfoReturnable<Boolean> cir)
    {
        if (!CarpetSettings.quasiConnectivity)
        {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

}
