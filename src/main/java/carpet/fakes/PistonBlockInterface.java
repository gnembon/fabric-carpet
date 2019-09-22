package carpet.fakes;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public interface PistonBlockInterface
{
    boolean publicShouldExtend(World world_1, BlockPos blockPos_1, Direction direction_1);
}
