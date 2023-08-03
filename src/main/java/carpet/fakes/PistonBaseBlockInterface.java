package carpet.fakes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public interface PistonBaseBlockInterface
{
    default boolean carpet$getNeighborSignal(Level level, BlockPos pos, Direction facing) { throw new UnsupportedOperationException(); }
}
