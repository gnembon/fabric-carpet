package carpet.fakes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public interface WorldChunkInterface
{
    BlockState setBlockStateWithBlockEntity(BlockPos blockPos, BlockState newBlockState, BlockEntity newBlockEntity, boolean boolean1);
}
