package carpet.helpers;

import carpet.CarpetSettings;
import carpet.fakes.ChunkGeneratorInterface;
import com.mojang.datafixers.util.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.ProcessorLists;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.JigsawConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleRandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FancyFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.feature.treedecorators.BeehiveDecorator;
import net.minecraft.world.level.levelgen.feature.trunkplacers.FancyTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

public class FeatureGenerator
{
    public static final Object boo = new Object();
    synchronized public static Boolean plop(String featureName, ServerLevel world, BlockPos pos)
    {
        Thing custom = featureMap.get(featureName);
        if (custom != null)
        {
            return custom.plop(world, pos);
        }
        ResourceLocation id = new ResourceLocation(featureName);
        ConfiguredStructureFeature<?, ?> structureFeature = world.registryAccess().registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY).get(id);
        if (structureFeature != null)
        {
            return plopAnywhere( structureFeature, world, pos, world.getChunkSource().getGenerator(), false);
        }

        ConfiguredFeature<?, ?> configuredFeature = world.registryAccess().registryOrThrow(Registry.CONFIGURED_FEATURE_REGISTRY).get(id);
        if (configuredFeature != null)
        {
            CarpetSettings.skipGenerationChecks.set(true);
            try
            {
                return configuredFeature.place(world, world.getChunkSource().getGenerator(), world.random, pos);
            }
            finally
            {
                CarpetSettings.skipGenerationChecks.set(false);
            }
        }
        StructureFeature<?> structure = Registry.STRUCTURE_FEATURE.get(id);
        if (structure != null)
        {
            ConfiguredStructureFeature<?,?> configuredStandard = getDefaultFeature(structure, world, pos);
            if (configuredStandard != null)
                return plopAnywhere(configuredStandard, world, pos, world.getChunkSource().getGenerator(), false);
        }
        Feature<?> feature = Registry.FEATURE.get(id);
        if (feature != null)
        {
            ConfiguredFeature<?,?> configuredStandard = getDefaultFeature(feature, world, pos, true);
            if (configuredStandard != null)
            {
                CarpetSettings.skipGenerationChecks.set(true);
                try
                {
                    return configuredStandard.place(world, world.getChunkSource().getGenerator(), world.random, pos);
                }
                finally
                {
                    CarpetSettings.skipGenerationChecks.set(false);
                }
            }
        }
        return null;
    }

    public static ConfiguredStructureFeature<?, ?> resolveConfiguredStructure(String name, ServerLevel world, BlockPos pos)
    {
        ResourceLocation id = new ResourceLocation(name);
        ConfiguredStructureFeature<?, ?> configuredStructureFeature =  world.registryAccess().registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY).get(id);
        if (configuredStructureFeature != null) return configuredStructureFeature;
        StructureFeature<?> structureFeature = Registry.STRUCTURE_FEATURE.get(id);
        if (structureFeature == null) return null;
        return getDefaultFeature(structureFeature, world, pos);
    }

    synchronized public static Boolean plopGrid(ConfiguredStructureFeature<?, ?> structureFeature, ServerLevel world, BlockPos pos)
    {
        return plopAnywhere( structureFeature, world, pos, world.getChunkSource().getGenerator(), true);
    }

    @FunctionalInterface
    private interface Thing
    {
        Boolean plop(ServerLevel world, BlockPos pos);
    }
    private static Thing simplePlop(ConfiguredFeature<?,?> feature)
    {
        return (w, p) -> {
            CarpetSettings.skipGenerationChecks.set(true);
            try
            {
                return feature.place(w, w.getChunkSource().getGenerator(), w.random, p);
            }
            finally
            {
                CarpetSettings.skipGenerationChecks.set(false);
            }
        };
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>> Thing simplePlop(F feature, FC config)
    {
        return simplePlop(new ConfiguredFeature<>(feature, config));
    }

    private static Thing simpleTree(TreeConfiguration config)
    {
        //config.ignoreFluidCheck();
        return simplePlop(new ConfiguredFeature(Feature.TREE, config));
    }

    private static Thing spawnCustomStructure(ConfiguredStructureFeature<?,?> structure)
    {
        return setupCustomStructure(structure,false);
    }
    private static Thing setupCustomStructure(ConfiguredStructureFeature<?,?> structure, boolean wireOnly)
        {
        return (w, p) -> plopAnywhere(structure, w, p, w.getChunkSource().getGenerator(), wireOnly);
    }

    public static Boolean spawn(String name, ServerLevel world, BlockPos pos)
    {
        if (featureMap.containsKey(name))
            return featureMap.get(name).plop(world, pos);
        return null;
    }

    private static ConfiguredStructureFeature<?, ?> getDefaultFeature(StructureFeature<?> structure, ServerLevel world, BlockPos pos)
    {
        // would be nice to have a way to grab structures of this type for position

        Holder<Biome> existingBiome = world.getBiome(pos);
        ConfiguredStructureFeature<?, ?> result = null;
        for (var confstr :  world.registryAccess().registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY).entrySet().stream().
                     filter(cS -> cS.getValue().feature == structure).map(Map.Entry::getValue).toList())
        {
            result = confstr;
            if (confstr.biomes.contains(existingBiome)) return result;
        }
        return result;
    }

    private static ConfiguredFeature<?, ?> getDefaultFeature(Feature<?> feature, ServerLevel world, BlockPos pos, boolean tryHard)
    {
        List<HolderSet<PlacedFeature>> configuredStepFeatures = world.getBiome(pos).value().getGenerationSettings().features();
        for (HolderSet<PlacedFeature> step: configuredStepFeatures)
            for (Holder<PlacedFeature> provider: step)
            {
                if (provider.value().feature().value().feature() == feature)
                    return provider.value().feature().value();
            }
        if (!tryHard) return null;
        return world.registryAccess().registryOrThrow(Registry.CONFIGURED_FEATURE_REGISTRY).entrySet().stream().
                filter(cS -> cS.getValue().feature() == feature).
                findFirst().map(Map.Entry::getValue).orElse(null);
    }

    public static <T extends FeatureConfiguration> StructureStart shouldStructureStartAt(ServerLevel world, BlockPos pos, ConfiguredStructureFeature<T, ?> structure, boolean computeBox)
    {
        long seed = world.getSeed();
        ChunkGenerator generator = world.getChunkSource().getGenerator();
        ChunkGeneratorInterface cgi = (ChunkGeneratorInterface) generator;
        List<StructurePlacement> structureConfig = cgi.getPlacementsForFeatureCM(structure);
        ChunkPos chunkPos = new ChunkPos(pos);
        boolean couldPlace = structureConfig.stream().anyMatch(p -> p.isFeatureChunk(generator, chunkPos.x, chunkPos.z));
        if (!couldPlace) return null;

        final HolderSet<Biome> structureBiomes = structure.biomes();

        if (!computeBox) {
            Holder<Biome> genBiome = generator.getNoiseBiome(QuartPos.fromBlock(pos.getX()), QuartPos.fromBlock(pos.getY()), QuartPos.fromBlock(pos.getZ()));
            if (structureBiomes.contains(genBiome) && structure.feature.canGenerate(
                    world.registryAccess(), generator, generator.getBiomeSource(), world.getStructureManager(),
                    seed, chunkPos, structure.config, world, structureBiomes::contains
            ))
            {
                return StructureStart.INVALID_START;
            }
        }
        else {
            final StructureStart filledStructure = structure.generate(
                    world.registryAccess(), generator, generator.getBiomeSource(), world.getStructureManager(),
                    seed, chunkPos, 0, world, structureBiomes::contains);
            if (filledStructure != null && filledStructure.isValid()) {
                return filledStructure;
            }
        }
        return null;
    }

    private static TreeConfiguration.TreeConfigurationBuilder createTree(Block block, Block block2, int i, int j, int k, int l) {
        return new TreeConfiguration.TreeConfigurationBuilder(BlockStateProvider.simple(block), new StraightTrunkPlacer(i, j, k), BlockStateProvider.simple(block2), new BlobFoliagePlacer(ConstantInt.of(l), ConstantInt.of(0), 3), new TwoLayersFeatureSize(1, 0, 1));
    }

    public static final Map<String, Thing> featureMap = new HashMap<>() {{

        put("oak_bees", simpleTree( createTree(Blocks.OAK_LOG, Blocks.OAK_LEAVES, 4, 2, 0, 2).ignoreVines().decorators(List.of(new BeehiveDecorator(1.00F))).build()));
        put("fancy_oak_bees", simpleTree( (new TreeConfiguration.TreeConfigurationBuilder(BlockStateProvider.simple(Blocks.OAK_LOG), new FancyTrunkPlacer(3, 11, 0), BlockStateProvider.simple(Blocks.OAK_LEAVES), new FancyFoliagePlacer(ConstantInt.of(2), ConstantInt.of(4), 4), new TwoLayersFeatureSize(0, 0, 0, OptionalInt.of(4)))).ignoreVines().decorators(List.of(new BeehiveDecorator(1.00F))).build()));
        put("birch_bees", simpleTree( createTree(Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES, 5, 2, 0, 2).ignoreVines().decorators(List.of(new BeehiveDecorator(1.00F))).build()));

        put("coral_tree", simplePlop(Feature.CORAL_TREE, FeatureConfiguration.NONE));

        put("coral_claw", simplePlop(Feature.CORAL_CLAW, FeatureConfiguration.NONE));
        put("coral_mushroom", simplePlop(Feature.CORAL_MUSHROOM, FeatureConfiguration.NONE));
        put("coral", simplePlop(Feature.SIMPLE_RANDOM_SELECTOR, new SimpleRandomFeatureConfiguration(HolderSet.direct(
                PlacementUtils.inlinePlaced(Feature.CORAL_TREE, FeatureConfiguration.NONE),
                PlacementUtils.inlinePlaced(Feature.CORAL_CLAW, FeatureConfiguration.NONE),
                PlacementUtils.inlinePlaced(Feature.CORAL_MUSHROOM, FeatureConfiguration.NONE)
        ))));
        put("bastion_remnant_units", spawnCustomStructure(StructureFeature.BASTION_REMNANT.configured(
                new JigsawConfiguration(Holder.direct(new StructureTemplatePool(
                        new ResourceLocation("bastion/starts"),
                        new ResourceLocation("empty"),
                        List.of(
                                Pair.of(StructurePoolElement.single("bastion/units/air_base", ProcessorLists.BASTION_GENERIC_DEGRADATION), 1)
                        ),
                        StructureTemplatePool.Projection.RIGID
                )), 6),
                BiomeTags.HAS_BASTION_REMNANT)
        ));
        put("bastion_remnant_hoglin_stable", spawnCustomStructure(StructureFeature.BASTION_REMNANT.configured(
                new JigsawConfiguration(Holder.direct(new StructureTemplatePool(
                        new ResourceLocation("bastion/starts"),
                        new ResourceLocation("empty"),
                        List.of(
                                Pair.of(StructurePoolElement.single("bastion/hoglin_stable/air_base", ProcessorLists.BASTION_GENERIC_DEGRADATION), 1)
                        ),
                        StructureTemplatePool.Projection.RIGID
                )), 6),
                BiomeTags.HAS_BASTION_REMNANT)
        ));
        put("bastion_remnant_treasure", spawnCustomStructure(StructureFeature.BASTION_REMNANT.configured(
                new JigsawConfiguration(Holder.direct(new StructureTemplatePool(
                        new ResourceLocation("bastion/starts"),
                        new ResourceLocation("empty"),
                        List.of(
                                Pair.of(StructurePoolElement.single("bastion/treasure/big_air_full", ProcessorLists.BASTION_GENERIC_DEGRADATION), 1)
                        ),
                        StructureTemplatePool.Projection.RIGID
                )), 6),
                BiomeTags.HAS_BASTION_REMNANT)
        ));
        put("bastion_remnant_bridge", spawnCustomStructure(StructureFeature.BASTION_REMNANT.configured(
                new JigsawConfiguration(Holder.direct(new StructureTemplatePool(
                        new ResourceLocation("bastion/starts"),
                        new ResourceLocation("empty"),
                        List.of(
                                Pair.of(StructurePoolElement.single("bastion/bridge/starting_pieces/entrance_base", ProcessorLists.BASTION_GENERIC_DEGRADATION), 1)
                        ),
                        StructureTemplatePool.Projection.RIGID
                )), 6),
                BiomeTags.HAS_BASTION_REMNANT)
        ));
    }};


    public static boolean plopAnywhere(ConfiguredStructureFeature<?, ?> structure, ServerLevel world, BlockPos pos, ChunkGenerator generator, boolean wireOnly)
    {
        if (world.isClientSide())
            return false;
        CarpetSettings.skipGenerationChecks.set(true);
        try
        {
            StructureStart start = structure.generate(world.registryAccess(), generator, generator.getBiomeSource(), world.getStructureManager(), world.getSeed(), new ChunkPos(pos), 0, world, b -> true );
            if (start == StructureStart.INVALID_START)
            {
                return false;
            }
            Random rand = new Random(world.getRandom().nextInt());
            int j = pos.getX() >> 4;
            int k = pos.getZ() >> 4;
            long chId = ChunkPos.asLong(j, k);

            world.getChunk(j, k).addReferenceForFeature(structure, chId);

            BoundingBox box = start.getBoundingBox();

            if (!wireOnly)
            {
                Registry<ConfiguredStructureFeature<?, ?>> registry3 = world.registryAccess().registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY);
                world.setCurrentlyGenerating(() -> {
                    Objects.requireNonNull(structure);
                    return registry3.getResourceKey(structure).map(Object::toString).orElseGet(structure::toString);
                });
                start.placeInChunk(world, world.structureFeatureManager(), generator, rand, box, new ChunkPos(j, k));
            }
            //structurestart.notifyPostProcessAt(new ChunkPos(j, k));
            int i = Math.max(box.getXSpan(),box.getZSpan())/16+1;

            //int i = getRadius();
            for (int k1 = j - i; k1 <= j + i; ++k1)
            {
                for (int l1 = k - i; l1 <= k + i; ++l1)
                {
                    if (k1 == j && l1 == k) continue;
                    if (box.intersects(k1<<4, l1<<4, (k1<<4) + 15, (l1<<4) + 15))
                    {
                        world.getChunk(k1, l1).addReferenceForFeature(structure, chId);
                    }
                }
            }
        }
        catch (Exception booboo)
        {
            CarpetSettings.LOG.error("Unknown Exception while plopping structure: "+booboo, booboo);
            return false;
        }
        finally
        {
            CarpetSettings.skipGenerationChecks.set(false);
        }
        return true;
    }
}
