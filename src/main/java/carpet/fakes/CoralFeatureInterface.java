package carpet.fakes;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface CoralFeatureInterface
{
    boolean growSpecific(Level worldIn, Random random, BlockPos pos, BlockState blockUnder);
}
