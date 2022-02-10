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
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.ProcessorLists;
import net.minecraft.data.worldgen.StructureFeatures;
import net.minecraft.data.worldgen.placement.PlacementUtils;
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
//import net.minecraft.world.level.levelgen.feature.configurations.StructureFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.JigsawConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleRandomFeatureConfiguration;
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
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
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
                    true, world.getServer().registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).get(net.minecraft.world.level.biome.Biomes.PLAINS), structureFeature.config);
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

    private static Thing simplePlop(Holder<PlacedFeature> hpf)
    {
        return simplePlop(hpf.value().feature().value());
    }

    private static Thing simpleTree(TreeConfiguration config)
    {
        //config.ignoreFluidCheck();
        return simplePlop(new ConfiguredFeature(Feature.TREE, config));
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
        var optinalBiome = world.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).getResourceKey(world.getBiome(pos).value());
        if (optinalBiome.isPresent())
            for (ResourceKey<ConfiguredStructureFeature<?, ?>> configureStructureKey: definedStructures.inverse().get(optinalBiome.get()))
            {
                var configureStructure = world.registryAccess().registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY).get(configureStructureKey);
                if (configureStructure.feature == structure)
                    return configureStructure;
            }
        if (!tryHard) return null;
        return world.registryAccess().registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY).entrySet().stream().
                filter(cS -> cS.getValue().feature == structure).
                findFirst().map(Map.Entry::getValue).orElse(null);
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

    public static <T extends FeatureConfiguration> StructureStart shouldStructureStartAt(ServerLevel world, BlockPos pos, StructureFeature<T> structure, boolean computeBox)
    {
        //if (structure == StructureFeature.STRONGHOLD)
        //    return shouldStrongholdStartAt(world, pos, computeBox);
        long seed = world.getSeed();
        ChunkGenerator generator = world.getChunkSource().getGenerator();
        StructureSettings settings = generator.getSettings();
        StructurePlacement structureConfig = settings.getConfig(structure);
        if (structureConfig instanceof ConcentricRingsStructurePlacement rings)
        {
            return shouldStrongholdStartAt(generator, rings, world, pos, computeBox);
        }
        var structures = settings.structures(structure);
        if (structureConfig == null || structures.isEmpty()) {
            return null;
        }
        ChunkPos chunkPos = new ChunkPos(pos);
        Holder<Biome> biome = world.getBiome(pos);
        Registry<Biome> biomeRegistry = world.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY);
        if (structures.values().stream().noneMatch(biomeKey -> biomeRegistry.getHolderOrThrow(biomeKey) == biome))
        {
            return null;
        }
        if (!structureConfig.isFeatureChunk(generator, chunkPos.x, chunkPos.z))
        //ChunkPos chunkPos1 = structure.getPotentialFeatureChunk(structureConfig, seed, chunkPos.x, chunkPos.z);
        //if (!chunkPos1.equals(chunkPos))
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

        var biomeConfig = structures.get(ResourceKey.create(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY, world.registryAccess().registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY).getKey(configuredFeature)));
        StructureStart<?> filledStructure = configuredFeature.generate(world.registryAccess(), generator, generator.getBiomeSource(),
                world.getStructureManager(), seed, chunkPos, 0, world, b -> biomeRegistry.getResourceKey(b.value()).filter(biomeConfig::contains).isPresent());
        if (filledStructure != null && filledStructure.isValid())
            return filledStructure;
        return null;
    }

    public static StructureStart<?> shouldStrongholdStartAt(ChunkGenerator generator, ConcentricRingsStructurePlacement rings, ServerLevel world, BlockPos pos, boolean computeBox)
    {
        final List<ChunkPos> positions = generator.getRingPositionsFor(rings);
        if (world.dimension() != Level.OVERWORLD || !positions.contains(new ChunkPos(pos))) return null;
        if (!computeBox) return StructureStart.INVALID_START;
        StructureSettings settings = generator.getSettings();
        StructurePlacement structureConfig = settings.getConfig(StructureFeature.STRONGHOLD);
        if (structureConfig != null) {
            final Predicate<ResourceKey<Biome>> biomeCheck = settings.structures(StructureFeature.STRONGHOLD).values().stream().collect(Collectors.toUnmodifiableSet())::contains;
            final Predicate<Holder<Biome>> holderCheck = b -> b.is(biomeCheck);
            StructureStart<?> filledStructure = StructureFeature.STRONGHOLD.generate(world.registryAccess(), generator, generator.getBiomeSource(),
                    world.getStructureManager(), world.getSeed(), new ChunkPos(pos), 0, NoneFeatureConfiguration.INSTANCE, world, holderCheck
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

        //put("coral_tree", simplePlop(Feature.CORAL_TREE.configured(FeatureConfiguration.NONE)));
        put("coral_tree", simplePlop(Feature.CORAL_TREE, FeatureConfiguration.NONE));

        put("coral_claw", simplePlop(Feature.CORAL_CLAW, FeatureConfiguration.NONE));
        put("coral_mushroom", simplePlop(Feature.CORAL_MUSHROOM, FeatureConfiguration.NONE));
        put("coral", simplePlop(Feature.SIMPLE_RANDOM_SELECTOR, new SimpleRandomFeatureConfiguration(HolderSet.direct(
                PlacementUtils.inlinePlaced(Feature.CORAL_TREE, FeatureConfiguration.NONE),
                PlacementUtils.inlinePlaced(Feature.CORAL_CLAW, FeatureConfiguration.NONE),
                PlacementUtils.inlinePlaced(Feature.CORAL_MUSHROOM, FeatureConfiguration.NONE)
        ))));
        put("bastion_remnant_units", spawnCustomStructure(
                StructureFeature.BASTION_REMNANT,
                new JigsawConfiguration(Holder.direct(new StructureTemplatePool(
                        new ResourceLocation("bastion/starts"),
                        new ResourceLocation("empty"),
                        List.of(
                                Pair.of(StructurePoolElement.single("bastion/units/air_base", ProcessorLists.BASTION_GENERIC_DEGRADATION), 1)
                        ),
                        StructureTemplatePool.Projection.RIGID
                )), 6),
                Biomes.NETHER_WASTES
        ));
        put("bastion_remnant_hoglin_stable", spawnCustomStructure(
                StructureFeature.BASTION_REMNANT,
                new JigsawConfiguration(Holder.direct(new StructureTemplatePool(
                        new ResourceLocation("bastion/starts"),
                        new ResourceLocation("empty"),
                        List.of(
                                Pair.of(StructurePoolElement.single("bastion/hoglin_stable/air_base", ProcessorLists.BASTION_GENERIC_DEGRADATION), 1)
                        ),
                        StructureTemplatePool.Projection.RIGID
                )), 6),
                Biomes.NETHER_WASTES
        ));
        put("bastion_remnant_treasure", spawnCustomStructure(
                StructureFeature.BASTION_REMNANT,
                new JigsawConfiguration(Holder.direct(new StructureTemplatePool(
                        new ResourceLocation("bastion/starts"),
                        new ResourceLocation("empty"),
                        List.of(
                                Pair.of(StructurePoolElement.single("bastion/treasure/big_air_full", ProcessorLists.BASTION_GENERIC_DEGRADATION), 1)
                        ),
                        StructureTemplatePool.Projection.RIGID
                )), 6),
                Biomes.NETHER_WASTES
        ));
        put("bastion_remnant_bridge", spawnCustomStructure(
                StructureFeature.BASTION_REMNANT,
                new JigsawConfiguration(Holder.direct(new StructureTemplatePool(
                        new ResourceLocation("bastion/starts"),
                        new ResourceLocation("empty"),
                        List.of(
                                Pair.of(StructurePoolElement.single("bastion/bridge/starting_pieces/entrance_base", ProcessorLists.BASTION_GENERIC_DEGRADATION), 1)
                        ),
                        StructureTemplatePool.Projection.RIGID
                )), 6),
                Biomes.NETHER_WASTES
        ));
    }};

}
