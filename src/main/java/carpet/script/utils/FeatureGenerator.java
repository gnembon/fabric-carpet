package carpet.script.utils;

import carpet.script.CarpetScriptServer;
import carpet.script.external.Vanilla;
import com.google.common.collect.ImmutableList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.data.worldgen.ProcessorLists;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleRandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FancyFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.feature.treedecorators.BeehiveDecorator;
import net.minecraft.world.level.levelgen.feature.trunkplacers.FancyTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

import javax.annotation.Nullable;

public class FeatureGenerator
{
    @Nullable
    public static synchronized Boolean plop(final String featureName, final ServerLevel world, final BlockPos pos)
    {
        final Function<ServerLevel, Thing> custom = featureMap.get(featureName);
        if (custom != null)
        {
            return custom.apply(world).plop(world, pos);
        }
        final ResourceLocation id = new ResourceLocation(featureName);
        final Structure structure = world.registryAccess().registryOrThrow(Registries.STRUCTURE).get(id);
        if (structure != null)
        {
            return plopAnywhere(structure, world, pos, world.getChunkSource().getGenerator(), false);
        }

        final ConfiguredFeature<?, ?> configuredFeature = world.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).get(id);
        if (configuredFeature != null)
        {
            final ThreadLocal<Boolean> checks = Vanilla.skipGenerationChecks(world);
            checks.set(true);
            try
            {
                return configuredFeature.place(world, world.getChunkSource().getGenerator(), world.random, pos);
            }
            finally
            {
                checks.set(false);
            }
        }
        final Optional<StructureType<?>> structureType = world.registryAccess().registryOrThrow(Registries.STRUCTURE_TYPE).getOptional(id);
        if (structureType.isPresent())
        {
            final Structure configuredStandard = getDefaultFeature(structureType.get(), world, pos);
            if (configuredStandard != null)
            {
                return plopAnywhere(configuredStandard, world, pos, world.getChunkSource().getGenerator(), false);
            }
        }
        final Feature<?> feature = world.registryAccess().registryOrThrow(Registries.FEATURE).get(id);
        if (feature != null)
        {
            final ConfiguredFeature<?, ?> configuredStandard = getDefaultFeature(feature, world, pos, true);
            if (configuredStandard != null)
            {
                final ThreadLocal<Boolean> checks = Vanilla.skipGenerationChecks(world);
                checks.set(true);
                try
                {
                    return configuredStandard.place(world, world.getChunkSource().getGenerator(), world.random, pos);
                }
                finally
                {
                    checks.set(false);
                }
            }
        }
        return null;
    }

    public static Structure resolveConfiguredStructure(final String name, final ServerLevel world, final BlockPos pos)
    {
        final ResourceLocation id = new ResourceLocation(name);
        final Structure configuredStructureFeature = world.registryAccess().registryOrThrow(Registries.STRUCTURE).get(id);
        if (configuredStructureFeature != null)
        {
            return configuredStructureFeature;
        }
        final StructureType<?> structureFeature = world.registryAccess().registryOrThrow(Registries.STRUCTURE_TYPE).get(id);
        if (structureFeature == null)
        {
            return null;
        }
        return getDefaultFeature(structureFeature, world, pos);
    }

    public static synchronized Boolean plopGrid(final Structure structureFeature, final ServerLevel world, final BlockPos pos)
    {
        return plopAnywhere(structureFeature, world, pos, world.getChunkSource().getGenerator(), true);
    }

    @FunctionalInterface
    private interface Thing
    {
        Boolean plop(ServerLevel world, BlockPos pos);
    }

    private static Thing simplePlop(final ConfiguredFeature<?, ?> feature)
    {
        return (w, p) -> {
            final ThreadLocal<Boolean> checks = Vanilla.skipGenerationChecks(w);
            checks.set(true);
            try
            {
                return feature.place(w, w.getChunkSource().getGenerator(), w.random, p);
            }
            finally
            {
                checks.set(false);
            }
        };
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>> Thing simplePlop(final F feature, final FC config)
    {
        return simplePlop(new ConfiguredFeature<>(feature, config));
    }

    private static Thing simpleTree(final TreeConfiguration config)
    {
        //config.ignoreFluidCheck();
        return simplePlop(new ConfiguredFeature<>(Feature.TREE, config));
    }

    private static Thing spawnCustomStructure(final Structure structure)
    {
        return setupCustomStructure(structure, false);
    }

    private static Thing setupCustomStructure(final Structure structure, final boolean wireOnly)
    {
        return (w, p) -> plopAnywhere(structure, w, p, w.getChunkSource().getGenerator(), wireOnly);
    }

    private static Structure getDefaultFeature(final StructureType<?> structure, final ServerLevel world, final BlockPos pos)
    {
        // would be nice to have a way to grab structures of this type for position
        // TODO allow old types, like vaillage, or bastion
        final Holder<Biome> existingBiome = world.getBiome(pos);
        Structure result = null;
        for (final Structure confstr : world.registryAccess().registryOrThrow(Registries.STRUCTURE).entrySet().stream().
                filter(cS -> cS.getValue().type() == structure).map(Map.Entry::getValue).toList())
        {
            result = confstr;
            if (confstr.biomes().contains(existingBiome))
            {
                return result;
            }
        }
        return result;
    }

    private static ConfiguredFeature<?, ?> getDefaultFeature(final Feature<?> feature, final ServerLevel world, final BlockPos pos, final boolean tryHard)
    {
        final List<HolderSet<PlacedFeature>> configuredStepFeatures = world.getBiome(pos).value().getGenerationSettings().features();
        for (final HolderSet<PlacedFeature> step : configuredStepFeatures)
        {
            for (final Holder<PlacedFeature> provider : step)
            {
                if (provider.value().feature().value().feature() == feature)
                {
                    return provider.value().feature().value();
                }
            }
        }
        if (!tryHard)
        {
            return null;
        }
        return world.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).entrySet().stream().
                filter(cS -> cS.getValue().feature() == feature).
                findFirst().map(Map.Entry::getValue).orElse(null);
    }

    public static <T extends FeatureConfiguration> StructureStart shouldStructureStartAt(final ServerLevel world, final BlockPos pos, final Structure structure, final boolean computeBox)
    {
        final ServerChunkCache chunkSource = world.getChunkSource();
        final RandomState seed = chunkSource.randomState();
        final ChunkGenerator generator = chunkSource.getGenerator();
        final ChunkGeneratorStructureState structureState = chunkSource.getGeneratorState();
        final List<StructurePlacement> structureConfig = structureState.getPlacementsForStructure(Holder.direct(structure));
        final ChunkPos chunkPos = new ChunkPos(pos);
        final boolean couldPlace = structureConfig.stream().anyMatch(p -> p.isStructureChunk(structureState, chunkPos.x, chunkPos.z));
        if (!couldPlace)
        {
            return null;
        }

        final HolderSet<Biome> structureBiomes = structure.biomes();

        if (!computeBox)
        {
            //Holder<Biome> genBiome = generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(pos.getX()), QuartPos.fromBlock(pos.getY()), QuartPos.fromBlock(pos.getZ()), seed.sampler());
            if (structure.findValidGenerationPoint(new Structure.GenerationContext(
                    world.registryAccess(), generator, generator.getBiomeSource(),
                    seed, world.getStructureManager(), world.getSeed(), chunkPos, world, structureBiomes::contains
            )).isPresent())
            {
                return StructureStart.INVALID_START;
            }
        }
        else
        {
            final StructureStart filledStructure = structure.generate(
                    world.registryAccess(), generator, generator.getBiomeSource(), seed, world.getStructureManager(),
                    world.getSeed(), chunkPos, 0, world, structureBiomes::contains);
            if (filledStructure != null && filledStructure.isValid())
            {
                return filledStructure;
            }
        }
        return null;
    }

    private static TreeConfiguration.TreeConfigurationBuilder createTree(final Block block, final Block block2, final int i, final int j, final int k, final int l)
    {
        return new TreeConfiguration.TreeConfigurationBuilder(BlockStateProvider.simple(block), new StraightTrunkPlacer(i, j, k), BlockStateProvider.simple(block2), new BlobFoliagePlacer(ConstantInt.of(l), ConstantInt.of(0), 3), new TwoLayersFeatureSize(1, 0, 1));
    }

    public static final Map<String, Function<ServerLevel, Thing>> featureMap = new HashMap<>()
    {{

        put("oak_bees", l -> simpleTree(createTree(Blocks.OAK_LOG, Blocks.OAK_LEAVES, 4, 2, 0, 2).ignoreVines().decorators(List.of(new BeehiveDecorator(1.00F))).build()));
        put("fancy_oak_bees", l -> simpleTree((new TreeConfiguration.TreeConfigurationBuilder(BlockStateProvider.simple(Blocks.OAK_LOG), new FancyTrunkPlacer(3, 11, 0), BlockStateProvider.simple(Blocks.OAK_LEAVES), new FancyFoliagePlacer(ConstantInt.of(2), ConstantInt.of(4), 4), new TwoLayersFeatureSize(0, 0, 0, OptionalInt.of(4)))).ignoreVines().decorators(List.of(new BeehiveDecorator(1.00F))).build()));
        put("birch_bees", l -> simpleTree(createTree(Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES, 5, 2, 0, 2).ignoreVines().decorators(List.of(new BeehiveDecorator(1.00F))).build()));

        put("coral_tree", l -> simplePlop(Feature.CORAL_TREE, FeatureConfiguration.NONE));

        put("coral_claw", l -> simplePlop(Feature.CORAL_CLAW, FeatureConfiguration.NONE));
        put("coral_mushroom", l -> simplePlop(Feature.CORAL_MUSHROOM, FeatureConfiguration.NONE));
        put("coral", l -> simplePlop(Feature.SIMPLE_RANDOM_SELECTOR, new SimpleRandomFeatureConfiguration(HolderSet.direct(
                PlacementUtils.inlinePlaced(Feature.CORAL_TREE, FeatureConfiguration.NONE),
                PlacementUtils.inlinePlaced(Feature.CORAL_CLAW, FeatureConfiguration.NONE),
                PlacementUtils.inlinePlaced(Feature.CORAL_MUSHROOM, FeatureConfiguration.NONE)
        ))));
        put("bastion_remnant_units", l -> {
            final RegistryAccess regs = l.registryAccess();

            final HolderGetter<StructureProcessorList> processorLists = regs.lookupOrThrow(Registries.PROCESSOR_LIST);
            final Holder<StructureProcessorList> bastionGenericDegradation = processorLists.getOrThrow(ProcessorLists.BASTION_GENERIC_DEGRADATION);

            final HolderGetter<StructureTemplatePool> pools = regs.lookupOrThrow(Registries.TEMPLATE_POOL);
            final Holder<StructureTemplatePool> empty = pools.getOrThrow(Pools.EMPTY);


            return spawnCustomStructure(
                    new JigsawStructure(new Structure.StructureSettings(
                            l.registryAccess().registryOrThrow(Registries.BIOME).getOrCreateTag(BiomeTags.HAS_BASTION_REMNANT),
                            Map.of(),
                            GenerationStep.Decoration.SURFACE_STRUCTURES,
                            TerrainAdjustment.NONE
                    ),
                            Holder.direct(new StructureTemplatePool(
                                    empty,
                                    ImmutableList.of(
                                            Pair.of(StructurePoolElement.single("bastion/units/air_base", bastionGenericDegradation), 1)
                                    ),
                                    StructureTemplatePool.Projection.RIGID
                            )),
                            6,
                            ConstantHeight.of(VerticalAnchor.absolute(33)),
                            false
                    )
            );
        });
        put("bastion_remnant_hoglin_stable", l -> {
            final RegistryAccess regs = l.registryAccess();

            final HolderGetter<StructureProcessorList> processorLists = regs.lookupOrThrow(Registries.PROCESSOR_LIST);
            final Holder<StructureProcessorList> bastionGenericDegradation = processorLists.getOrThrow(ProcessorLists.BASTION_GENERIC_DEGRADATION);

            final HolderGetter<StructureTemplatePool> pools = regs.lookupOrThrow(Registries.TEMPLATE_POOL);
            final Holder<StructureTemplatePool> empty = pools.getOrThrow(Pools.EMPTY);

            return spawnCustomStructure(
                    new JigsawStructure(new Structure.StructureSettings(
                            l.registryAccess().registryOrThrow(Registries.BIOME).getOrCreateTag(BiomeTags.HAS_BASTION_REMNANT),
                            Map.of(),
                            GenerationStep.Decoration.SURFACE_STRUCTURES,
                            TerrainAdjustment.NONE
                    ),
                            Holder.direct(new StructureTemplatePool(
                                    empty,
                                    ImmutableList.of(
                                            Pair.of(StructurePoolElement.single("bastion/hoglin_stable/air_base", bastionGenericDegradation), 1)
                                    ),
                                    StructureTemplatePool.Projection.RIGID
                            )),
                            6,
                            ConstantHeight.of(VerticalAnchor.absolute(33)),
                            false
                    )
            );
        });
        put("bastion_remnant_treasure", l -> {
            final RegistryAccess regs = l.registryAccess();

            final HolderGetter<StructureProcessorList> processorLists = regs.lookupOrThrow(Registries.PROCESSOR_LIST);
            final Holder<StructureProcessorList> bastionGenericDegradation = processorLists.getOrThrow(ProcessorLists.BASTION_GENERIC_DEGRADATION);

            final HolderGetter<StructureTemplatePool> pools = regs.lookupOrThrow(Registries.TEMPLATE_POOL);
            final Holder<StructureTemplatePool> empty = pools.getOrThrow(Pools.EMPTY);

            return spawnCustomStructure(
                    new JigsawStructure(new Structure.StructureSettings(
                            l.registryAccess().registryOrThrow(Registries.BIOME).getOrCreateTag(BiomeTags.HAS_BASTION_REMNANT),
                            Map.of(),
                            GenerationStep.Decoration.SURFACE_STRUCTURES,
                            TerrainAdjustment.NONE
                    ),
                            Holder.direct(new StructureTemplatePool(
                                    empty,
                                    ImmutableList.of(
                                            Pair.of(StructurePoolElement.single("bastion/treasure/big_air_full", bastionGenericDegradation), 1)
                                    ),
                                    StructureTemplatePool.Projection.RIGID
                            )),
                            6,
                            ConstantHeight.of(VerticalAnchor.absolute(33)),
                            false
                    )
            );
        });
        put("bastion_remnant_bridge", l -> {
            final RegistryAccess regs = l.registryAccess();

            final HolderGetter<StructureProcessorList> processorLists = regs.lookupOrThrow(Registries.PROCESSOR_LIST);
            final Holder<StructureProcessorList> bastionGenericDegradation = processorLists.getOrThrow(ProcessorLists.BASTION_GENERIC_DEGRADATION);

            final HolderGetter<StructureTemplatePool> pools = regs.lookupOrThrow(Registries.TEMPLATE_POOL);
            final Holder<StructureTemplatePool> empty = pools.getOrThrow(Pools.EMPTY);

            return spawnCustomStructure(
                    new JigsawStructure(new Structure.StructureSettings(
                            l.registryAccess().registryOrThrow(Registries.BIOME).getOrCreateTag(BiomeTags.HAS_BASTION_REMNANT),
                            Map.of(),
                            GenerationStep.Decoration.SURFACE_STRUCTURES,
                            TerrainAdjustment.NONE
                    ),
                            Holder.direct(new StructureTemplatePool(
                                    empty,
                                    ImmutableList.of(
                                            Pair.of(StructurePoolElement.single("bastion/bridge/starting_pieces/entrance_base", bastionGenericDegradation), 1)
                                    ),
                                    StructureTemplatePool.Projection.RIGID
                            )),
                            6,
                            ConstantHeight.of(VerticalAnchor.absolute(33)),
                            false
                    )
            );
        });
    }};


    public static boolean plopAnywhere(final Structure structure, final ServerLevel world, final BlockPos pos, final ChunkGenerator generator, final boolean wireOnly)
    {
        final ThreadLocal<Boolean> checks = Vanilla.skipGenerationChecks(world);
        checks.set(true);
        try
        {
            final StructureStart start = structure.generate(world.registryAccess(), generator, generator.getBiomeSource(), world.getChunkSource().randomState(), world.getStructureManager(), world.getSeed(), new ChunkPos(pos), 0, world, b -> true);
            if (start == StructureStart.INVALID_START)
            {
                return false;
            }
            final RandomSource rand = RandomSource.create(world.getRandom().nextInt());
            final int j = pos.getX() >> 4;
            final int k = pos.getZ() >> 4;
            final long chId = ChunkPos.asLong(j, k);
            world.getChunk(j, k).setStartForStructure(structure, start);
            world.getChunk(j, k).addReferenceForStructure(structure, chId);

            final BoundingBox box = start.getBoundingBox();

            if (!wireOnly)
            {
                final Registry<Structure> registry3 = world.registryAccess().registryOrThrow(Registries.STRUCTURE);
                world.setCurrentlyGenerating(() -> {
                    Objects.requireNonNull(structure);
                    return registry3.getResourceKey(structure).map(Object::toString).orElseGet(structure::toString);
                });
                start.placeInChunk(world, world.structureManager(), generator, rand, box, new ChunkPos(j, k));
            }
            //structurestart.notifyPostProcessAt(new ChunkPos(j, k));
            final int i = Math.max(box.getXSpan(), box.getZSpan()) / 16 + 1;

            //int i = getRadius();
            for (int k1 = j - i; k1 <= j + i; ++k1)
            {
                for (int l1 = k - i; l1 <= k + i; ++l1)
                {
                    if (k1 == j && l1 == k)
                    {
                        continue;
                    }
                    if (box.intersects(k1 << 4, l1 << 4, (k1 << 4) + 15, (l1 << 4) + 15))
                    {
                        world.getChunk(k1, l1).addReferenceForStructure(structure, chId);
                    }
                }
            }
        }
        catch (final Exception booboo)
        {
            CarpetScriptServer.LOG.error("Unknown Exception while plopping structure: " + booboo, booboo);
            return false;
        }
        finally
        {
            checks.set(false);
        }
        return true;
    }
}
