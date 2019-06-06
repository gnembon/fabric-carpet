package carpet.fakes;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.BlockPos;

import java.util.Map;

public interface WorldInterface
{
    Map<EntityType, Entity> getPrecookedMobs();
    
    boolean setBlockStateWithBlockEntity(BlockPos blockPos, BlockState blockState, BlockEntity newBlockEntity, int int1);
}
