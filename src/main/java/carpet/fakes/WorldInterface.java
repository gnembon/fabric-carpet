package carpet.fakes;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public interface WorldInterface
{
    Map<EntityType<?>, Entity> getPrecookedMobs();
    
    boolean setBlockStateWithBlockEntity(BlockPos blockPos, BlockState blockState, BlockEntity newBlockEntity, int int1);

    List<Entity> getOtherEntitiesLimited(@Nullable Entity except, Box box, Predicate<? super Entity> predicate, int limit);
}
