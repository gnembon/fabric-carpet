package carpet.fakes;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

public interface WorldChunkInterface
{
    BlockState setBlockStateWithBlockEntity(BlockPos blockPos, BlockState newBlockState, BlockEntity newBlockEntity, boolean boolean1);
    int getEntityCount(int from, int to);
    Entity getEntityAtIndex(int index, int from, int to);
    //boolean checkModified();
}
