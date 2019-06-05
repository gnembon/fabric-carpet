package carpet.fakes;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

public interface WorldChunkInterface
{
    BlockState setBlockStateWithBlockEntity(BlockPos blockPos, BlockState newBlockState, BlockEntity newBlockEntity, boolean boolean1);
}
