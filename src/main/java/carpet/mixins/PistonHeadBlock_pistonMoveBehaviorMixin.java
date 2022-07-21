package carpet.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(PistonHeadBlock.class)
public class PistonHeadBlock_pistonMoveBehaviorMixin {

    @Inject(
        method = "onRemove",
        cancellable = true,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockBehaviour;onRemove(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Z"
        )
    )
    private void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved, CallbackInfo ci) {
        if (moved) {
            ci.cancel();
        }
    }
}
