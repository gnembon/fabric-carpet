package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelChunk.class)
public class LevelChunk_fillUpdatesMixin
{
    // todo onStateReplaced needs a bit more love since it removes be which is needed
    @Redirect(method = "setBlockState", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;onPlace(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)V"
    ))
    private void onAdded(BlockState blockState, Level world_1, BlockPos blockPos_1, BlockState blockState_1, boolean boolean_1)
    {
        if (!CarpetSettings.impendingFillSkipUpdates.get())
            blockState.onPlace(world_1, blockPos_1, blockState_1, boolean_1);
    }

    @Redirect(method = "setBlockState", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/BlockEntity;preRemoveSideEffects(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)V"
    ))
    private void onPreRemoveSideEffects(BlockEntity blockEntity, BlockPos blockPos_1, BlockState blockState_1, boolean boolean_1)
    {
        if (!CarpetSettings.impendingFillSkipUpdates.get())
        {
            blockEntity.preRemoveSideEffects(blockPos_1, blockState_1, boolean_1);
        }
    }

    @Redirect(method = "setBlockState", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;affectNeighborsAfterRemoval(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Z)V"
    ))
    private void onAffectNeighborsAfterRemoval(final BlockState instance, final ServerLevel serverLevel, final BlockPos blockPos, final boolean b)
    {
        if (!CarpetSettings.impendingFillSkipUpdates.get())
        {
            instance.affectNeighborsAfterRemoval(serverLevel, blockPos, b);
        }
    }
}
