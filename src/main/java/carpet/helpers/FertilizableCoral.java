package carpet.helpers;

import carpet.script.utils.FeatureGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseCoralPlantTypeBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.CoralClawFeature;
import net.minecraft.world.level.levelgen.feature.CoralTreeFeature;
import net.minecraft.world.level.levelgen.feature.CuboidPlacement;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.SimpleRandomSelectorFeature;
import net.minecraft.world.level.levelgen.placement.OffsetPlacement;
import net.minecraft.world.level.levelgen.placement.RandomChancePlacement;
import net.minecraft.world.level.material.MapColor;

import java.util.stream.Stream;

/**
 * Deduplicates logic for the different behaviors of the {@code renewableCoral} rule
 */
public interface FertilizableCoral extends BonemealableBlock {
    /**
     * @return Whether the rule for this behavior is enabled
     */
    boolean isEnabled();

    @Override
    public default boolean isValidBonemealTarget(LevelReader world, BlockPos pos, BlockState state, BonemealSource bonemealSource)
    {
        return isEnabled()
                && state.getValue(BaseCoralPlantTypeBlock.WATERLOGGED)
                && world.getFluidState(pos.above()).is(FluidTags.WATER);
    }

    @Override
    public default boolean isBonemealSuccess(Level world, RandomSource random, BlockPos pos, BlockState state, BonemealSource bonemealSource)
    {
        return random.nextFloat() < 0.15D;
    }

    @Override
    public default void performBonemeal(ServerLevel worldIn, RandomSource random, BlockPos pos, BlockState blockUnder, BonemealSource bonemealSource)
    {
        MapColor color = blockUnder.getMapColor(worldIn, pos);
        BlockState properBlock = blockUnder;
        var blocks = worldIn.registryAccess().lookupOrThrow(Registries.BLOCK);
        HolderSet.Named<Block> coralBlocks = worldIn.registryAccess().lookupOrThrow(Registries.BLOCK).get(BlockTags.CORAL_BLOCKS).orElseThrow();
        for (Holder<Block> block: coralBlocks)
        {
            properBlock = block.value().defaultBlockState();
            if (properBlock.getMapColor(worldIn, pos) == color)
            {
                break;
            }
        }
        Block actualProperBlock = properBlock.getBlock();

        worldIn.setBlock(pos, Blocks.WATER.defaultBlockState(), Block.UPDATE_NONE);

        var features = worldIn.registryAccess().lookupOrThrow(Registries.FEATURE);
        Feature feature = new SimpleRandomSelectorFeature( HolderSet.direct(Stream.of(actualProperBlock)
                .map(block -> FeatureGenerator.coral(features, blocks, actualProperBlock))
                .flatMap(
                        coralType -> Stream.of(
                                PlacementUtils.inlinePlaced(new CoralTreeFeature(
                                        PlacementUtils.inlinePlaced(coralType, FeatureGenerator.coralPlacement))
                                ),
                                PlacementUtils.inlinePlaced(new CoralClawFeature(
                                        PlacementUtils.inlinePlaced(coralType, FeatureGenerator.coralPlacement))),
                                PlacementUtils.inlinePlaced(
                                        coralType,
                                        OffsetPlacement.vertical(UniformInt.of(-3, -1)),
                                        new CuboidPlacement(
                                                UniformInt.of(3, 5),
                                                UniformInt.of(3, 5),
                                                false,
                                                false
                                        ),
                                        new RandomChancePlacement(0.9f),
                                        FeatureGenerator.coralPlacement
                                )
                        )
                ).toList())
        );
        if (!feature.place(worldIn, worldIn.getChunkSource().getGenerator(), random, pos))
        {
            worldIn.setBlock(pos, blockUnder, 3);
        }
        else
        {
            if (worldIn.getRandom().nextInt(10) == 0)
            {
                BlockPos randomPos = pos.offset(worldIn.getRandom().nextInt(16) - 8, worldIn.getRandom().nextInt(8), worldIn.getRandom().nextInt(16) - 8);
                if (worldIn.getBlockState(randomPos).is(BlockTags.CORAL_BLOCKS))
                {
                    worldIn.setBlock(randomPos, Blocks.WET_SPONGE.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }
}
