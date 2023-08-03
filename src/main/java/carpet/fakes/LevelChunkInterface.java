package carpet.fakes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public interface LevelChunkInterface
{
    default BlockState carpet$setBlockStateWithBlockEntity(BlockPos pos, BlockState state, BlockEntity blockEntity, boolean movedByPiston) { throw new UnsupportedOperationException(); }
}
