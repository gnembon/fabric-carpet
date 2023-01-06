package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
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
            target = "Lnet/minecraft/world/level/block/state/BlockState;onRemove(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)V"
    ))
    private void onRemovedBlock(BlockState blockState, Level world, BlockPos pos, BlockState state, boolean moved)
    {
        if (CarpetSettings.impendingFillSkipUpdates.get()) // doing due dilligence from AbstractBlock onStateReplaced
        {
            if (blockState.hasBlockEntity() && !blockState.is(state.getBlock()))
            {
                world.removeBlockEntity(pos);
            }
        }
        else
        {
            blockState.onRemove(world, pos, state, moved);
        }
    }
}
