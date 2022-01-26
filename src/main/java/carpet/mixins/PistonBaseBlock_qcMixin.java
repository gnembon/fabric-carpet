package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonBaseBlock.class)
public class PistonBaseBlock_qcMixin
{
    @Inject(method = "getNeighborSignal", cancellable = true, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/core/BlockPos;above()Lnet/minecraft/core/BlockPos;"
    ))
    private void cancelUpCheck(Level world_1, BlockPos blockPos_1, Direction direction_1, CallbackInfoReturnable<Boolean> cir)
    {
        if (!CarpetSettings.quasiConnectivity)
        {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

}
