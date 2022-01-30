package carpet.mixins;

import carpet.CarpetSettings;
import carpet.fakes.CoralFeatureInterface;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseCoralPlantTypeBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CoralFanBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.CoralClawFeature;
import net.minecraft.world.level.levelgen.feature.CoralFeature;
import net.minecraft.world.level.levelgen.feature.CoralMushroomFeature;
import net.minecraft.world.level.levelgen.feature.CoralTreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.MaterialColor;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Random;

@Mixin(CoralFanBlock.class)
public abstract class CoralFanBlock_renewableCoralMixin implements BonemealableBlock
{
    public boolean isValidBonemealTarget(BlockGetter var1, BlockPos var2, BlockState var3, boolean var4)
    {
        return CarpetSettings.renewableCoral == CarpetSettings.RenewableCoralMode.EXPANDED
                && var3.getValue(BaseCoralPlantTypeBlock.WATERLOGGED)
                && var1.getFluidState(var2.above()).is(FluidTags.WATER);
    }

    public boolean isBonemealSuccess(Level var1, Random var2, BlockPos var3, BlockState var4)
    {
        return (double)var1.random.nextFloat() < 0.15D;
    }

    public void performBonemeal(ServerLevel worldIn, Random random, BlockPos pos, BlockState blockUnder)
    {

        CoralFeature coral;
        int variant = random.nextInt(3);
        if (variant == 0)
            coral = new CoralClawFeature(NoneFeatureConfiguration.CODEC);
        else if (variant == 1)
            coral = new CoralTreeFeature(NoneFeatureConfiguration.CODEC);
        else
            coral = new CoralMushroomFeature(NoneFeatureConfiguration.CODEC);

        MaterialColor color = blockUnder.getMapColor(worldIn, pos);
        BlockState proper_block = blockUnder;
        for (Block block: BlockTags.CORAL_BLOCKS.getValues())
        {
            proper_block = block.defaultBlockState();
            if (proper_block.getMapColor(worldIn,pos) == color)
            {
                break;
            }
        }
        worldIn.setBlock(pos, Blocks.WATER.defaultBlockState(), 4);

        if (!((CoralFeatureInterface)coral).growSpecific(worldIn, random, pos, proper_block))
        {
            worldIn.setBlock(pos, blockUnder, 3);
        }
        else
        {
            if (worldIn.random.nextInt(10)==0)
            {
                BlockPos randomPos = pos.offset(worldIn.random.nextInt(16)-8,worldIn.random.nextInt(8),worldIn.random.nextInt(16)-8  );
                if (BlockTags.CORAL_BLOCKS.contains(worldIn.getBlockState(randomPos).getBlock()))
                {
                    worldIn.setBlock(randomPos, Blocks.WET_SPONGE.defaultBlockState(), 3);
                }
            }
        }
    }
}
