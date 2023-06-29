package carpet.fakes;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface CoralFeatureInterface
{
    boolean growSpecific(Level worldIn, RandomSource random, BlockPos pos, BlockState blockUnder);
}
