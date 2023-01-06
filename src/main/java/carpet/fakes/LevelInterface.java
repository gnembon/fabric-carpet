package carpet.fakes;

import net.minecraft.world.level.redstone.NeighborUpdater;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public interface LevelInterface
{
    Map<EntityType<?>, Entity> getPrecookedMobs();
    
    boolean setBlockStateWithBlockEntity(BlockPos blockPos, BlockState blockState, BlockEntity newBlockEntity, int int1);

    List<Entity> getOtherEntitiesLimited(@Nullable Entity except, AABB box, Predicate<? super Entity> predicate, int limit);

    NeighborUpdater getNeighborUpdater();
}
