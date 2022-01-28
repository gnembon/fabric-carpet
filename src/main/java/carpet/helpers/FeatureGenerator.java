package carpet.helpers;

import carpet.CarpetSettings;
import carpet.fakes.ChunkGeneratorInterface;
import carpet.fakes.PlacedFeatureInterface;
import carpet.fakes.StructureFeatureInterface;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMultimap;
import com.mojang.datafixers.util.Pair;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.ProcessorLists;
import net.minecraft.data.worldgen.StructureFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.StructureSettings;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.JigsawConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleRandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.StructureFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FancyFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.structures.StructurePoolElement;
import net.minecraft.world.level.levelgen.feature.structures.StructureTemplatePool;
import net.minecraft.world.level.levelgen.feature.treedecorators.BeehiveDecorator;
import net.minecraft.world.level.levelgen.feature.trunkplacers.FancyTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureStart;

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
            return ((StructureFeatureInterface)structureFeature.feature).plopAnywhere(
                    world, pos, world.getChunkSource().getGenerator(),
                    false, world.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).get(Biomes.PLAINS), structureFeature.config);

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
            ConfiguredStructureFeature<?,?> configuredStandard = getDefaultFeature(structure, world, pos, true);
            if (configuredStandard != null)
                return ((StructureFeatureInterface)configuredStandard.feature).plopAnywhere(
                        world, pos, world.getChunkSource().getGenerator(),
                        false, world.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).get(Biomes.PLAINS), configuredStandard.config);

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
        return getDefaultFeature(structureFeature, world, pos, true);
    }

    synchronized public static Boolean plopGrid(ConfiguredStructureFeature<?, ?> structureFeature, ServerLevel world, BlockPos pos)
    {
        return ((StructureFeatureInterface)structureFeature.feature).plopAnywhere(
                    world, pos, world.getChunkSource().getGenerator(),
                    true, net.minecraft.data.worldgen.biome.Biomes.PLAINS, structureFeature.config);
    }

    @FunctionalInterface
    private interface Thing
    {
        Boolean plop(ServerLevel world, BlockPos pos);
    }
    private static Thing simplePlop(ConfiguredFeature feature)
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

    private static Thing simpleTree(TreeConfiguration config)
    {
        //config.ignoreFluidCheck();
        return simplePlop(Feature.TREE.configured(config));
    }

    private static Thing spawnCustomStructure(StructureFeature structure, FeatureConfiguration conf, ResourceKey<Biome> biome)
    {
        return setupCustomStructure(structure, conf, biome, false);
    }
    private static Thing setupCustomStructure(StructureFeature structure, FeatureConfiguration conf, ResourceKey<Biome> biome, boolean wireOnly)
        {
        return (w, p) -> ((StructureFeatureInterface)structure).plopAnywhere(w, p, w.getChunkSource().getGenerator(), wireOnly, w.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).get(biome), conf);
    }

    public static Boolean spawn(String name, ServerLevel world, BlockPos pos)
    {
        if (featureMap.containsKey(name))
            return featureMap.get(name).plop(world, pos);
        return null;
    }

    private static ConfiguredStructureFeature<?, ?> getDefaultFeature(StructureFeature<?> structure, ServerLevel world, BlockPos pos, boolean tryHard)
    {
        var definedStructures = world.getChunkSource().getGenerator().getSettings().structures(structure);
        var optinalBiome = world.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).getResourceKey(world.getBiome(pos));
        if (optinalBiome.isPresent())
            for (var configureStructure: definedStructures.inverse().get(optinalBiome.get()))
                if (configureStructure.feature == structure)
                    return configureStructure;
        if (!tryHard) return null;
        return world.registryAccess().registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY).entrySet().stream().
                filter(cS -> cS.getValue().feature == structure).
                findFirst().map(Map.Entry::getValue).orElse(null);
    }

    private static ConfiguredFeature<?, ?> getDefaultFeature(Feature<?> feature, ServerLevel world, BlockPos pos, boolean tryHard)
    {
        List<List<Supplier<PlacedFeature>>> configuredStepFeatures = world.getBiome(pos).getGenerationSettings().features();
        for (List<Supplier<PlacedFeature>> step: configuredStepFeatures)
            for (Supplier<PlacedFeature> provider: step)
            {
                ConfiguredFeature<?, ?> configuredFeature = ((PlacedFeatureInterface)provider.get()).getRawFeature();
                if (configuredFeature.feature == feature)
                    return configuredFeature;
            }
        if (!tryHard) return null;
        return world.registryAccess().registryOrThrow(Registry.CONFIGURED_FEATURE_REGISTRY).entrySet().stream().
                filter(cS -> cS.getValue().feature == feature).
                findFirst().map(Map.Entry::getValue).orElse(null);
    }

    public static <T extends FeatureConfiguration> StructureStart shouldStructureStartAt(ServerLevel world, BlockPos pos, StructureFeature<T> structure, boolean computeBox)
    {
        if (structure == StructureFeature.STRONGHOLD)
            return shouldStrongholdStartAt(world, pos, computeBox);
        long seed = world.getSeed();
        ChunkGenerator generator = world.getChunkSource().getGenerator();
        StructureSettings settings = generator.getSettings();
        StructureFeatureConfiguration structureConfig = settings.getConfig(structure);
        var structures = settings.structures(structure);
        if (structureConfig == null || structures.isEmpty()) {
            return null;
        }
        ChunkPos chunkPos = new ChunkPos(pos);
        Biome biome = world.getBiome(pos);
        var biomeRegistry = world.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY);
        if (structures.values().stream().noneMatch(biomeKey -> biomeRegistry.get(biomeKey) == biome))
        {
            return null;
        }
        ChunkPos chunkPos1 = structure.getPotentialFeatureChunk(structureConfig, seed, chunkPos.x, chunkPos.z);
        if (!chunkPos1.equals(chunkPos))
        {
            return null;
        }
        StructureFeatureManager structureManager = world.structureFeatureManager();
        StructureCheckResult isThere =  structureManager.checkStructurePresence(chunkPos, structure, false);
        if (isThere == StructureCheckResult.START_NOT_PRESENT)
        {
            return null;
        }
        // gen - we want to avoig, right?
        //Chunk chunk = world.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.STRUCTURE_STARTS);
        //StructureStart<?> start =  structureManager.getStructureStart(ChunkSectionPos.from(chunk), structure, chunk);
        //if (start != null && start.hasChildren())
        //{
        if (!computeBox) return StructureStart.INVALID_START;
        ConfiguredStructureFeature<?, ?> configuredFeature = getDefaultFeature(structure, world, pos, false);
        if (configuredFeature == null || configuredFeature.config == null) return null;
        var biomeConfig = structures.get(configuredFeature);
        StructureStart<?> filledStructure = configuredFeature.generate(world.registryAccess(), generator, generator.getBiomeSource(),
                world.getStructureManager(), seed, chunkPos, 0, structureConfig, world, (b) -> biomeRegistry.getResourceKey(b).filter(biomeConfig::contains).isPresent());
        if (filledStructure != null && filledStructure.isValid())
            return filledStructure;
        return null;
    }

    public static StructureStart<?> shouldStrongholdStartAt(ServerLevel world, BlockPos pos, boolean computeBox)
    {
        ChunkGenerator generator = world.getChunkSource().getGenerator();
        if (world.dimension() != Level.OVERWORLD || !generator.hasStronghold(new ChunkPos(pos))) return null;
        if (!computeBox) return StructureStart.INVALID_START;
        StructureSettings settings = generator.getSettings();
        StructureFeatureConfiguration structureConfig = settings.getConfig(StructureFeature.STRONGHOLD);
        if (structureConfig != null) {
            StructureStart<?> filledStructure = StructureFeatures.STRONGHOLD.generate(world.registryAccess(), generator, generator.getBiomeSource(),
                    world.getStructureManager(), world.getSeed(), new ChunkPos(pos), 0,
                    structureConfig, world, ((ChunkGeneratorInterface)generator)::canPlaceStrongholdInBiomeCM
            );
            if (filledStructure != null && filledStructure.isValid())
                return filledStructure;
        }
        return null;
    }

    private static TreeConfiguration.TreeConfigurationBuilder createTree(Block block, Block block2, int i, int j, int k, int l) {
        return new TreeConfiguration.TreeConfigurationBuilder(BlockStateProvider.simple(block), new StraightTrunkPlacer(i, j, k), BlockStateProvider.simple(block2), new BlobFoliagePlacer(ConstantInt.of(l), ConstantInt.of(0), 3), new TwoLayersFeatureSize(1, 0, 1));
    }

    public static final Map<String, Thing> featureMap = new HashMap<String, Thing>() {{

        put("oak_bees", simpleTree( createTree(Blocks.OAK_LOG, Blocks.OAK_LEAVES, 4, 2, 0, 2).ignoreVines().decorators(List.of(new BeehiveDecorator(1.00F))).build()));
        put("fancy_oak_bees", simpleTree( (new TreeConfiguration.TreeConfigurationBuilder(BlockStateProvider.simple(Blocks.OAK_LOG), new FancyTrunkPlacer(3, 11, 0), BlockStateProvider.simple(Blocks.OAK_LEAVES), new FancyFoliagePlacer(ConstantInt.of(2), ConstantInt.of(4), 4), new TwoLayersFeatureSize(0, 0, 0, OptionalInt.of(4)))).ignoreVines().decorators(List.of(new BeehiveDecorator(1.00F))).build()));
        put("birch_bees", simpleTree( createTree(Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES, 5, 2, 0, 2).ignoreVines().decorators(List.of(new BeehiveDecorator(1.00F))).build()));

        put("coral_tree", simplePlop(Feature.CORAL_TREE.configured(FeatureConfiguration.NONE)));
        put("coral_claw", simplePlop(Feature.CORAL_CLAW.configured(FeatureConfiguration.NONE)));
        put("coral_mushroom", simplePlop(Feature.CORAL_MUSHROOM.configured(FeatureConfiguration.NONE)));
        put("coral", simplePlop(Feature.SIMPLE_RANDOM_SELECTOR.configured(new SimpleRandomFeatureConfiguration(List.of(
                () -> Feature.CORAL_TREE.configured(FeatureConfiguration.NONE).placed(),
                () -> Feature.CORAL_CLAW.configured(FeatureConfiguration.NONE).placed(),
                () -> Feature.CORAL_MUSHROOM.configured(FeatureConfiguration.NONE).placed()
        )))));
        put("bastion_remnant_units", spawnCustomStructure(
                StructureFeature.BASTION_REMNANT,
                new JigsawConfiguration(() -> new StructureTemplatePool(
                        new ResourceLocation("bastion/starts"),
                        new ResourceLocation("empty"),
                        List.of(
                                Pair.of(StructurePoolElement.single("bastion/units/air_base", ProcessorLists.BASTION_GENERIC_DEGRADATION), 1)
                        ),
                        StructureTemplatePool.Projection.RIGID
                ), 6),
                Biomes.NETHER_WASTES
        ));
        put("bastion_remnant_hoglin_stable", spawnCustomStructure(
                StructureFeature.BASTION_REMNANT,
                new JigsawConfiguration(() -> new StructureTemplatePool(
                        new ResourceLocation("bastion/starts"),
                        new ResourceLocation("empty"),
                        List.of(
                                Pair.of(StructurePoolElement.single("bastion/hoglin_stable/air_base", ProcessorLists.BASTION_GENERIC_DEGRADATION), 1)
                        ),
                        StructureTemplatePool.Projection.RIGID
                ), 6),
                Biomes.NETHER_WASTES
        ));
        put("bastion_remnant_treasure", spawnCustomStructure(
                StructureFeature.BASTION_REMNANT,
                new JigsawConfiguration(() -> new StructureTemplatePool(
                        new ResourceLocation("bastion/starts"),
                        new ResourceLocation("empty"),
                        List.of(
                                Pair.of(StructurePoolElement.single("bastion/treasure/big_air_full", ProcessorLists.BASTION_GENERIC_DEGRADATION), 1)
                        ),
                        StructureTemplatePool.Projection.RIGID
                ), 6),
                Biomes.NETHER_WASTES
        ));
        put("bastion_remnant_bridge", spawnCustomStructure(
                StructureFeature.BASTION_REMNANT,
                new JigsawConfiguration(() -> new StructureTemplatePool(
                        new ResourceLocation("bastion/starts"),
                        new ResourceLocation("empty"),
                        List.of(
                                Pair.of(StructurePoolElement.single("bastion/bridge/starting_pieces/entrance_base", ProcessorLists.BASTION_GENERIC_DEGRADATION), 1)
                        ),
                        StructureTemplatePool.Projection.RIGID
                ), 6),
                Biomes.NETHER_WASTES
        ));
    }};

}
