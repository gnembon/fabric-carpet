package carpet.mixins;

import net.minecraft.world.level.SignalGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import carpet.helpers.QuasiConnectivity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.piston.PistonBaseBlock;

@Mixin(PistonBaseBlock.class)
public class PistonBaseBlock_qcMixin {

    @Inject(
        method = "getNeighborSignal",
        cancellable = true,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/core/BlockPos;above()Lnet/minecraft/core/BlockPos;"
        )
    )
    private void carpet_checkQuasiSignal(SignalGetter level, BlockPos pos, Direction facing, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(QuasiConnectivity.hasQuasiSignal(level, pos));
    }
}
