package carpet.fakes;

import carpet.helpers.TickRateManager;
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
    default Map<EntityType<?>, Entity> carpet$getPrecookedMobs() { throw new UnsupportedOperationException(); }

    default boolean carpet$setBlockStateWithBlockEntity(BlockPos pos, BlockState state, BlockEntity blockEntity, int flags) { throw new UnsupportedOperationException(); }

    default List<Entity> carpet$getOtherEntitiesLimited(@Nullable Entity except, AABB box, Predicate<? super Entity> predicate, int limit) { throw new UnsupportedOperationException(); }

    default NeighborUpdater carpet$getNeighborUpdater() { throw new UnsupportedOperationException(); }

    default TickRateManager carpet$getTickRateManager() { throw new UnsupportedOperationException(); }
}
