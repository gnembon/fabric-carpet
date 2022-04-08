package carpet.mixins;

import carpet.fakes.CoralFeatureInterface;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.CoralFeature;

@Mixin(CoralFeature.class)
public abstract class CoralFeature_renewableCoralMixin implements CoralFeatureInterface
{

    @Shadow protected abstract boolean placeFeature(LevelAccessor var1, RandomSource var2, BlockPos var3, BlockState var4);

    @Override
    public boolean growSpecific(Level worldIn, RandomSource random, BlockPos pos, BlockState blockUnder)
    {
        return placeFeature(worldIn, random, pos, blockUnder);
    }
}
