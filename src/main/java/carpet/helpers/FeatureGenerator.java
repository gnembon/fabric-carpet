package carpet.helpers;

import carpet.CarpetSettings;
import carpet.fakes.StructureFeatureInterface;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureManager;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.structure.processor.StructureProcessorLists;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.BuiltinBiomes;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.StructureConfig;
import net.minecraft.world.gen.tree.BeehiveTreeDecorator;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.ConfiguredFeatures;
import net.minecraft.world.gen.feature.ConfiguredStructureFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.SimpleRandomFeatureConfig;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.gen.feature.StructurePoolFeatureConfig;
import net.minecraft.world.gen.feature.TreeFeatureConfig;

import java.util.HashMap;
import java.util.Map;

public class FeatureGenerator
{
    public static final Object boo = new Object();
    synchronized public static Boolean plop(String featureName, ServerWorld world, BlockPos pos)
    {
        Thing custom = featureMap.get(featureName);
        if (custom != null)
        {
            return custom.plop(world, pos);
        }
        Identifier id = new Identifier(featureName);
        ConfiguredStructureFeature<?, ?> structureFeature = world.getRegistryManager().get(Registry.CONFIGURED_STRUCTURE_FEATURE_WORLDGEN).get(id);
        if (structureFeature != null)
        {
            return ((StructureFeatureInterface)structureFeature.feature).plopAnywhere(
                    world, pos, world.getChunkManager().getChunkGenerator(),
                    false, world.getRegistryManager().get(Registry.BIOME_KEY).get(BiomeKeys.PLAINS), structureFeature.config);

        }

        ConfiguredFeature<?, ?> feature = world.getRegistryManager().get(Registry.CONFIGURED_FEATURE_WORLDGEN).get(id);
        if (feature != null)
        {
            CarpetSettings.skipGenerationChecks.set(true);
            try
            {
                return feature.generate(world, world.getChunkManager().getChunkGenerator(), world.random, pos);
            }
            finally
            {
                CarpetSettings.skipGenerationChecks.set(false);
            }
        }
        return null;
    }

    public static ConfiguredStructureFeature<?, ?> resolveConfiguredStructure(String name, ServerWorld world, BlockPos pos)
    {
        Identifier id = new Identifier(name);
        ConfiguredStructureFeature<?, ?> configuredStructureFeature =  world.getRegistryManager().get(Registry.CONFIGURED_STRUCTURE_FEATURE_WORLDGEN).get(id);
        if (configuredStructureFeature != null) return configuredStructureFeature;
        StructureFeature<?> structureFeature = Registry.STRUCTURE_FEATURE.get(id);
        if (structureFeature == null) return null;
        return getDefaultFeature(structureFeature, world, pos, true);
    }

    synchronized public static Boolean plopGrid(ConfiguredStructureFeature<?, ?> structureFeature, ServerWorld world, BlockPos pos)
    {
        return ((StructureFeatureInterface)structureFeature.feature).plopAnywhere(
                    world, pos, world.getChunkManager().getChunkGenerator(),
                    true, BuiltinBiomes.PLAINS, structureFeature.config);
    }

    @FunctionalInterface
    private interface Thing
    {
        Boolean plop(ServerWorld world, BlockPos pos);
    }
    private static Thing simplePlop(ConfiguredFeature feature)
    {
        return (w, p) -> {
            CarpetSettings.skipGenerationChecks.set(true);
            try
            {
                return feature.generate(w, w.getChunkManager().getChunkGenerator(), w.random, p);
            }
            finally
            {
                CarpetSettings.skipGenerationChecks.set(false);
            }
        };
    }

    private static Thing simpleTree(TreeFeatureConfig config)
    {
        config.ignoreFluidCheck();
        return simplePlop(Feature.TREE.configure(config));
    }

    private static Thing spawnCustomStructure(StructureFeature structure, FeatureConfig conf, RegistryKey<Biome> biome)
    {
        return setupCustomStructure(structure, conf, biome, false);
    }
    private static Thing setupCustomStructure(StructureFeature structure, FeatureConfig conf, RegistryKey<Biome> biome, boolean wireOnly)
        {
        return (w, p) -> ((StructureFeatureInterface)structure).plopAnywhere(w, p, w.getChunkManager().getChunkGenerator(), wireOnly, w.getRegistryManager().get(Registry.BIOME_KEY).get(biome), conf);
    }

    public static Boolean spawn(String name, ServerWorld world, BlockPos pos)
    {
        if (featureMap.containsKey(name))
            return featureMap.get(name).plop(world, pos);
        return null;
    }

    private static ConfiguredStructureFeature<?, ?> getDefaultFeature(StructureFeature<?> structure, ServerWorld world, BlockPos pos, boolean tryHard)
    {
        ConfiguredStructureFeature<?, ?> configuredFeature = world.getBiome(pos).getGenerationSettings().method_30978(structure.configure(null));
        if (configuredFeature.config != null || !tryHard) return configuredFeature;
        return world.getRegistryManager().get(Registry.CONFIGURED_STRUCTURE_FEATURE_WORLDGEN).getEntries().stream().
                filter(cS -> cS.getValue().feature == structure).
                findFirst().map(Map.Entry::getValue).orElse(null);
    }

    public static <T extends FeatureConfig> StructureStart shouldStructureStartAt(ServerWorld world, BlockPos pos, StructureFeature<T> structure, boolean computeBox)
    {
        long seed = world.getSeed();
        ChunkGenerator generator = world.getChunkManager().getChunkGenerator();
        StructureConfig params = generator.getStructuresConfig().getForType(structure);
        synchronized(boo) {
            if (!generator.getBiomeSource().hasStructureFeature(structure))
                return null;
        }
        BiomeAccess biomeAccess = world.getBiomeAccess().withSource(generator.getBiomeSource());
        ChunkRandom chunkRandom = new ChunkRandom();
        ChunkPos chunkPos = new ChunkPos(pos);
        Biome biome = biomeAccess.getBiome(new BlockPos(chunkPos.getStartX() + 9, 0, chunkPos.getStartZ() + 9));
        ConfiguredStructureFeature<?, ?> configuredFeature = biome.getGenerationSettings().method_30978(structure.configure(null));
        if (configuredFeature == null || configuredFeature.config == null) return null;
        ChunkPos chunkPos2 = structure.getStartChunk(params, seed, chunkRandom, chunkPos.x, chunkPos.z); //find some chunk I guess
        if (chunkPos.x == chunkPos2.x && chunkPos.z == chunkPos2.z && ((StructureFeatureInterface)structure).shouldStartPublicAt(generator, generator.getBiomeSource(), seed, chunkRandom, chunkPos.x, chunkPos.z, biome, chunkPos, configuredFeature.config)) // should start at
        {
            if (!computeBox) return StructureStart.DEFAULT;
            StructureManager manager = world.getStructureManager();
            StructureStart<T> structureStart3 = structure.getStructureStartFactory().create((StructureFeature<T>) configuredFeature.feature, chunkPos.x, chunkPos.z, BlockBox.empty(), 0, seed);
            synchronized (boo) {
                structureStart3.init(world.getRegistryManager(), generator, manager, chunkPos.x, chunkPos.z, biome, (T) configuredFeature.config);
            }
            if (!structureStart3.hasChildren()) return null;
            return structureStart3;
        }
        return null;
    }

    public static final Map<String, Thing> featureMap = new HashMap<String, Thing>() {{
        put("oak_bees", simpleTree(ConfiguredFeatures.OAK.getConfig().setTreeDecorators(ImmutableList.of(new BeehiveTreeDecorator(1.0F)))));
        put("fancy_oak_bees", simpleTree(ConfiguredFeatures.FANCY_OAK.getConfig().setTreeDecorators(ImmutableList.of(new BeehiveTreeDecorator(1.0F)))));
        put("birch_bees", simpleTree(ConfiguredFeatures.BIRCH.getConfig().setTreeDecorators(ImmutableList.of(new BeehiveTreeDecorator(1.0F)))));
        put("coral_tree", simplePlop(Feature.CORAL_TREE.configure(FeatureConfig.DEFAULT)));
        put("coral_claw", simplePlop(Feature.CORAL_CLAW.configure(FeatureConfig.DEFAULT)));
        put("coral_mushroom", simplePlop(Feature.CORAL_MUSHROOM.configure(FeatureConfig.DEFAULT)));
        put("coral", simplePlop(Feature.SIMPLE_RANDOM_SELECTOR.configure(new SimpleRandomFeatureConfig(ImmutableList.of(
                () -> Feature.CORAL_TREE.configure(FeatureConfig.DEFAULT),
                () -> Feature.CORAL_CLAW.configure(FeatureConfig.DEFAULT),
                () -> Feature.CORAL_MUSHROOM.configure(FeatureConfig.DEFAULT)
        )))));
        put("bastion_remnant_units", spawnCustomStructure(
                StructureFeature.BASTION_REMNANT,
                new StructurePoolFeatureConfig(() -> new StructurePool(
                        new Identifier("bastion/starts"),
                        new Identifier("empty"),
                        ImmutableList.of(
                                Pair.of(StructurePoolElement.method_30435("bastion/units/air_base", StructureProcessorLists.BASTION_GENERIC_DEGRADATION), 1)
                        ),
                        StructurePool.Projection.RIGID
                ), 6),
                BiomeKeys.NETHER_WASTES
        ));
        put("bastion_remnant_hoglin_stable", spawnCustomStructure(
                StructureFeature.BASTION_REMNANT,
                new StructurePoolFeatureConfig(() -> new StructurePool(
                        new Identifier("bastion/starts"),
                        new Identifier("empty"),
                        ImmutableList.of(
                                Pair.of(StructurePoolElement.method_30435("bastion/hoglin_stable/air_base", StructureProcessorLists.BASTION_GENERIC_DEGRADATION), 1)
                        ),
                        StructurePool.Projection.RIGID
                ), 6),
                BiomeKeys.NETHER_WASTES
        ));
        put("bastion_remnant_treasure", spawnCustomStructure(
                StructureFeature.BASTION_REMNANT,
                new StructurePoolFeatureConfig(() -> new StructurePool(
                        new Identifier("bastion/starts"),
                        new Identifier("empty"),
                        ImmutableList.of(
                                Pair.of(StructurePoolElement.method_30435("bastion/treasure/big_air_full", StructureProcessorLists.BASTION_GENERIC_DEGRADATION), 1)
                        ),
                        StructurePool.Projection.RIGID
                ), 6),
                BiomeKeys.NETHER_WASTES
        ));
        put("bastion_remnant_bridge", spawnCustomStructure(
                StructureFeature.BASTION_REMNANT,
                new StructurePoolFeatureConfig(() -> new StructurePool(
                        new Identifier("bastion/starts"),
                        new Identifier("empty"),
                        ImmutableList.of(
                                Pair.of(StructurePoolElement.method_30435("bastion/bridge/starting_pieces/entrance_base", StructureProcessorLists.BASTION_GENERIC_DEGRADATION), 1)
                        ),
                        StructurePool.Projection.RIGID
                ), 6),
                BiomeKeys.NETHER_WASTES
        ));
    }};

}
