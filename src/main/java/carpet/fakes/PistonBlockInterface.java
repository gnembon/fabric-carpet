package carpet.fakes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public interface PistonBlockInterface
{
    boolean publicShouldExtend(Level world_1, BlockPos blockPos_1, Direction direction_1);
}
