package carpet.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import carpet.helpers.QuasiConnectivity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(DispenserBlock.class)
public class DispenserBlock_qcMixin {

    @Redirect(
        method = "neighborChanged",
        at = @At(
            value = "INVOKE",
            ordinal = 1,
            target =  "Lnet/minecraft/world/level/Level;hasNeighborSignal(Lnet/minecraft/core/BlockPos;)Z"
        )
    )
    private boolean carpet_hasQuasiSignal(Level _level, BlockPos above, BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        return QuasiConnectivity.hasQuasiSignal(level, pos);
    }
}
