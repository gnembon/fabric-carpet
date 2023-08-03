package carpet.mixins;

import carpet.fakes.CoralFeatureInterface;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.CoralFeature;

@Mixin(CoralFeature.class)
public abstract class CoralFeature_renewableCoralMixin implements CoralFeatureInterface
{

    @Shadow protected abstract boolean placeFeature(LevelAccessor level, RandomSource random, BlockPos pos, BlockState blockUnder);

    @Override
    public boolean carpet$growSpecific(Level level, RandomSource random, BlockPos pos, BlockState blockUnder)
    {
        return placeFeature(level, random, pos, blockUnder);
    }
}
