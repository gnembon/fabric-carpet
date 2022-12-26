package carpet.helpers;

import carpet.fakes.CoralFeatureInterface;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseCoralPlantTypeBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.CoralClawFeature;
import net.minecraft.world.level.levelgen.feature.CoralFeature;
import net.minecraft.world.level.levelgen.feature.CoralMushroomFeature;
import net.minecraft.world.level.levelgen.feature.CoralTreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.MaterialColor;

/**
 * Deduplicates logic for the different behaviors of the {@code renewableCoral} rule
 */
public interface FertilizableCoral extends BonemealableBlock {
    /**
     * @return Whether the rule for this behavior is enabled
     */
    boolean isEnabled();

    @Override
    public default boolean isValidBonemealTarget(LevelReader world, BlockPos pos, BlockState state, boolean var4)
    {
        return isEnabled()
                && state.getValue(BaseCoralPlantTypeBlock.WATERLOGGED)
                && world.getFluidState(pos.above()).is(FluidTags.WATER);
    }

    @Override
    public default boolean isBonemealSuccess(Level world, RandomSource random, BlockPos pos, BlockState state)
    {
        return random.nextFloat() < 0.15D;
    }

    @Override
    public default void performBonemeal(ServerLevel worldIn, RandomSource random, BlockPos pos, BlockState blockUnder)
    {
        int variant = random.nextInt(3);
        CoralFeature coral = switch (variant) {
            case 0 -> new CoralClawFeature(NoneFeatureConfiguration.CODEC);
            case 1 -> new CoralTreeFeature(NoneFeatureConfiguration.CODEC);
            default -> new CoralMushroomFeature(NoneFeatureConfiguration.CODEC);
        };

        MaterialColor color = blockUnder.getMapColor(worldIn, pos);
        BlockState properBlock = blockUnder;
        HolderSet.Named<Block> coralBlocks = worldIn.registryAccess().registryOrThrow(Registries.BLOCK).getTag(BlockTags.CORAL_BLOCKS).orElseThrow();
        for (Holder<Block> block: coralBlocks)
        {
            properBlock = block.value().defaultBlockState();
            if (properBlock.getMapColor(worldIn, pos) == color)
            {
                break;
            }
        }
        worldIn.setBlock(pos, Blocks.WATER.defaultBlockState(), Block.UPDATE_NONE);

        if (!((CoralFeatureInterface)coral).growSpecific(worldIn, random, pos, properBlock))
        {
            worldIn.setBlock(pos, blockUnder, 3);
        }
        else
        {
            if (worldIn.random.nextInt(10) == 0)
            {
                BlockPos randomPos = pos.offset(worldIn.random.nextInt(16) - 8, worldIn.random.nextInt(8), worldIn.random.nextInt(16) - 8);
                if (coralBlocks.contains(worldIn.getBlockState(randomPos).getBlockHolder()))
                {
                    worldIn.setBlock(randomPos, Blocks.WET_SPONGE.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }
}
