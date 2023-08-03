package carpet.fakes;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface CoralFeatureInterface
{
    default boolean carpet$growSpecific(Level level, RandomSource random, BlockPos pos, BlockState blockUnder) { throw new UnsupportedOperationException(); }
}
