package carpet.helpers;

import carpet.CarpetSettings;
import carpet.fakes.BiomeInterface;
import carpet.fakes.StructureFeatureInterface;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.block.Blocks;
import net.minecraft.class_5314;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureManager;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.structure.BastionRemnantGenerator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.biome.DefaultBiomeFeatures;
import net.minecraft.world.biome.source.BiomeAccess;
//import net.minecraft.world.biome.source.BiomeSource;
//import net.minecraft.world.biome.source.FixedBiomeSource;
//import net.minecraft.world.biome.source.VanillaLayeredBiomeSource;
//import net.minecraft.world.biome.source.VanillaLayeredBiomeSourceConfig;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.chunk.ChunkGenerator;
//import net.minecraft.world.gen.chunk.OverworldChunkGenerator;
//import net.minecraft.world.gen.chunk.OverworldChunkGeneratorConfig;
import net.minecraft.world.gen.decorator.BeehiveTreeDecorator;
import net.minecraft.world.gen.feature.BastionRemnantFeatureConfig;
import net.minecraft.world.gen.feature.BoulderFeatureConfig;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.ConfiguredStructureFeature;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.HugeFungusFeatureConfig;
import net.minecraft.world.gen.feature.MineshaftFeature;
import net.minecraft.world.gen.feature.MineshaftFeatureConfig;
import net.minecraft.world.gen.feature.OceanRuinFeature;
import net.minecraft.world.gen.feature.OceanRuinFeatureConfig;
import net.minecraft.world.gen.feature.RandomPatchFeatureConfig;
import net.minecraft.world.gen.feature.SeaPickleFeatureConfig;
import net.minecraft.world.gen.feature.SeagrassFeatureConfig;
import net.minecraft.world.gen.feature.ShipwreckFeatureConfig;
import net.minecraft.world.gen.feature.SingleStateFeatureConfig;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.gen.feature.StructurePoolFeatureConfig;
import net.minecraft.world.gen.feature.TreeFeatureConfig;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static net.minecraft.world.biome.DefaultBiomeFeatures.FANCY_TREE_CONFIG;
import static net.minecraft.world.biome.DefaultBiomeFeatures.OAK_TREE_CONFIG;

public class FeatureGenerator
{
    public static String fix(String key)
    {
        return key.toLowerCase(Locale.ROOT);
    }

    @FunctionalInterface
    private interface Thing
    {
        Boolean plop(ServerWorld world, BlockPos pos);
    }
    private static Thing simplePlop(ConfiguredFeature feature)
    {
        return (w, p) -> {
            CarpetSettings.skipGenerationChecks=true;
            try
            {
                return feature.generate(w, w.getStructureAccessor(), w.getChunkManager().getChunkGenerator(), w.random, p);
            }
            finally
            {
                CarpetSettings.skipGenerationChecks = false;
            }
        };
    }
    private static Thing simplePlop(Feature<DefaultFeatureConfig> feature)
    {
        return simplePlop(feature.configure(FeatureConfig.DEFAULT));
    }

    private static Thing simpleTree(TreeFeatureConfig config)
    {
        config.ignoreFluidCheck();
        return simplePlop(Feature.TREE.configure(config));
    }

    private static Thing simplePatch(RandomPatchFeatureConfig config)
    {
        return simplePlop(Feature.RANDOM_PATCH.configure(config));
    }

    private static Thing spawnCustomStructure(StructureFeature structure, FeatureConfig conf, Biome biome)
    {
        return setupCustomStructure(structure, conf, biome, false);
    }
    private static Thing gridCustomStructure(StructureFeature structure, FeatureConfig conf, Biome biome)
    {
        return setupCustomStructure(structure, conf, biome, true);
    }
    private static Thing setupCustomStructure(StructureFeature structure, FeatureConfig conf, Biome biome, boolean wireOnly)
        {
        return (w, p) -> ((StructureFeatureInterface)structure).plopAnywhere(w, p, w.getChunkManager().getChunkGenerator(), wireOnly, biome, conf);
    }

    public static Boolean spawn(String name, ServerWorld world, BlockPos pos)
    {
        if (featureMap.containsKey(name))
            return featureMap.get(name).plop(world, pos);
        return null;
    }
    public static void noop() {System.out.println("boo");}

    public static Boolean gridStructure(String name, ServerWorld world, BlockPos pos)
    {
        if (gridMap.containsKey(name))
            return gridMap.get(name).plop(world, pos);
        return null;
    }

    public static <T extends FeatureConfig> StructureStart shouldStructureStartAt(ServerWorld world, BlockPos pos, StructureFeature<T> structure, boolean computeBox)
    {
        long seed = world.getSeed();
        ChunkGenerator generator = world.getChunkManager().getChunkGenerator();
        class_5314 params = generator.getConfig().method_28600(structure);
        if (!generator.getBiomeSource().hasStructureFeature(structure))
            return null;
        BiomeAccess biomeAccess = world.getBiomeAccess().withSource(generator.getBiomeSource());
        ChunkRandom chunkRandom = new ChunkRandom();
        ChunkPos chunkPos = new ChunkPos(pos);
        Biome biome = biomeAccess.getBiome(new BlockPos(chunkPos.getStartX() + 9, 0, chunkPos.getStartZ() + 9));
        ConfiguredStructureFeature<?, ?> configuredFeature = ((BiomeInterface)biome).getConfiguredFeature(structure);
        if (configuredFeature == null) return null;
        ChunkPos chunkPos2 = structure.method_27218(params, seed, chunkRandom, chunkPos.x, chunkPos.z); //find some chunk I guess
        if (chunkPos.x == chunkPos2.x && chunkPos.z == chunkPos2.z && ((StructureFeatureInterface)structure).shouldStartPublicAt(generator, generator.getBiomeSource(), seed, chunkRandom, chunkPos.x, chunkPos.z, biome, chunkPos, configuredFeature.field_24836)) // should start at
        {
            if (!computeBox) return StructureStart.DEFAULT;
            StructureManager manager = world.getStructureManager();
            StructureStart<T> structureStart3 = structure.getStructureStartFactory().create((StructureFeature<T>) configuredFeature.field_24835, chunkPos.x, chunkPos.z, BlockBox.empty(), 0, seed);
            structureStart3.init(generator, manager, chunkPos.x, chunkPos.z, biome, (T) configuredFeature.field_24836);
            if (!structureStart3.hasChildren()) return null;
            return structureStart3;
        }
        return null;
    }


    public static final Map<String, List<String>> structureToFeature = new HashMap<>();
    public static final Map<String, String> featureToStructure = new HashMap<>();
    static
    {
        structureToFeature.put(StructureFeature.MONUMENT.getName(), Collections.singletonList("monument"));
        structureToFeature.put(StructureFeature.END_CITY.getName(), Collections.singletonList("end_city"));
        structureToFeature.put(StructureFeature.OCEAN_RUIN.getName(), Arrays.asList("ocean_ruin", "ocean_ruin_warm", "ocean_ruin_small", "ocean_ruin_warm_small", "ocean_ruin_tall", "ocean_ruin_warm_tall"));
        structureToFeature.put(StructureFeature.VILLAGE.getName(), Arrays.asList("village", "village_desert", "village_savanna", "village_taiga", "village_snowy"));
        structureToFeature.put(StructureFeature.MANSION.getName(), Collections.singletonList("mansion"));
        structureToFeature.put(StructureFeature.BURIED_TREASURE.getName(), Collections.singletonList("treasure"));
        structureToFeature.put(StructureFeature.field_24851.getName(), Collections.singletonList("witch_hut"));
        structureToFeature.put(StructureFeature.STRONGHOLD.getName(), Collections.singletonList("stronghold"));
        structureToFeature.put(StructureFeature.DESERT_PYRAMID.getName(), Collections.singletonList("desert_temple"));
        structureToFeature.put(StructureFeature.JUNGLE_PYRAMID.getName(), Collections.singletonList("jungle_temple"));
        structureToFeature.put(StructureFeature.SHIPWRECK.getName(), Arrays.asList("shipwreck", "shipwreck2"));
        structureToFeature.put(StructureFeature.PILLAGER_OUTPOST.getName(), Collections.singletonList("pillager_outpost"));
        structureToFeature.put(StructureFeature.MINESHAFT.getName(), Arrays.asList("mineshaft", "mineshaft_mesa"));
        structureToFeature.put(StructureFeature.IGLOO.getName(), Collections.singletonList("igloo"));
        structureToFeature.put(StructureFeature.FORTRESS.getName(), Collections.singletonList("fortress"));
        structureToFeature.put(StructureFeature.NETHER_FOSSIL.getName(), Collections.singletonList("nether_fossil"));
        structureToFeature.put(StructureFeature.BASTION_REMNANT.getName(), Arrays.asList("bastion_remnant", "bastion_remnant_housing", "bastion_remnant_stable", "bastion_remnant_treasure", "bastion_remnant_bridge"));
        structureToFeature.put(StructureFeature.RUINED_PORTAL.getName(), Collections.singletonList("ruined_portal"));

        structureToFeature.forEach((key, value) -> value.forEach(el -> featureToStructure.put(el, key)));
    }


    private static final Map<String, Thing> gridMap = new HashMap<String, Thing>() {{
        put("monument",  ((StructureFeatureInterface)StructureFeature.MONUMENT)::gridAnywhere);
        put("fortress", ((StructureFeatureInterface)StructureFeature.FORTRESS)::gridAnywhere);
        put("mansion", ((StructureFeatureInterface)StructureFeature.MANSION)::gridAnywhere);
        put("jungle_temple", ((StructureFeatureInterface)StructureFeature.JUNGLE_PYRAMID)::gridAnywhere);
        put("desert_temple", ((StructureFeatureInterface)StructureFeature.DESERT_PYRAMID)::gridAnywhere);
        put("end_city", ((StructureFeatureInterface)StructureFeature.END_CITY)::gridAnywhere);
        put("igloo", ((StructureFeatureInterface)StructureFeature.IGLOO)::gridAnywhere);
        put("shipwreck", gridCustomStructure(StructureFeature.SHIPWRECK, new ShipwreckFeatureConfig(true), Biomes.PLAINS));
        put("shipwreck2", gridCustomStructure(StructureFeature.SHIPWRECK, new ShipwreckFeatureConfig(false), Biomes.PLAINS));
        put("witch_hut", ((StructureFeatureInterface)StructureFeature.field_24851)::gridAnywhere);
        put("stronghold", ((StructureFeatureInterface)StructureFeature.STRONGHOLD)::gridAnywhere);

        put("ocean_ruin_small", gridCustomStructure(StructureFeature.OCEAN_RUIN,
                new OceanRuinFeatureConfig(OceanRuinFeature.BiomeType.COLD, 0.0F, 0.5F), Biomes.PLAINS));
        put("ocean_ruin_warm_small", gridCustomStructure(StructureFeature.OCEAN_RUIN,
                new OceanRuinFeatureConfig(OceanRuinFeature.BiomeType.WARM, 0.0F, 0.5F), Biomes.PLAINS));
        put("ocean_ruin_tall", gridCustomStructure(StructureFeature.OCEAN_RUIN,
                new OceanRuinFeatureConfig(OceanRuinFeature.BiomeType.COLD, 1.0F, 0.0F), Biomes.PLAINS));
        put("ocean_ruin_warm_tall", gridCustomStructure(StructureFeature.OCEAN_RUIN,
                new OceanRuinFeatureConfig(OceanRuinFeature.BiomeType.WARM, 1.0F, 0.0F), Biomes.PLAINS));
        put("ocean_ruin", gridCustomStructure(StructureFeature.OCEAN_RUIN,
                new OceanRuinFeatureConfig(OceanRuinFeature.BiomeType.COLD, 1.0F, 1.0F), Biomes.PLAINS));
        put("ocean_ruin_warm", gridCustomStructure(StructureFeature.OCEAN_RUIN,
                new OceanRuinFeatureConfig(OceanRuinFeature.BiomeType.WARM, 1.0F, 1.0F), Biomes.PLAINS));
        put("treasure", ((StructureFeatureInterface)StructureFeature.BURIED_TREASURE)::gridAnywhere);

        put("pillager_outpost", ((StructureFeatureInterface)StructureFeature.PILLAGER_OUTPOST)::gridAnywhere);


        put("mineshaft", gridCustomStructure(StructureFeature.MINESHAFT, new MineshaftFeatureConfig(0.0, MineshaftFeature.Type.NORMAL), Biomes.PLAINS));
        put("mineshaft_mesa", gridCustomStructure(StructureFeature.MINESHAFT, new MineshaftFeatureConfig(0.0, MineshaftFeature.Type.MESA), Biomes.PLAINS));

        put("village", gridCustomStructure(StructureFeature.VILLAGE, new StructurePoolFeatureConfig(new Identifier("village/plains/town_centers"),6), Biomes.PLAINS));
        put("village_desert", gridCustomStructure(StructureFeature.VILLAGE, new StructurePoolFeatureConfig(new Identifier("village/desert/town_centers"), 6), Biomes.PLAINS));
        put("village_savanna", gridCustomStructure(StructureFeature.VILLAGE, new StructurePoolFeatureConfig(new Identifier("village/savanna/town_centers"), 6), Biomes.PLAINS));
        put("village_taiga", gridCustomStructure(StructureFeature.VILLAGE, new StructurePoolFeatureConfig(new Identifier("village/taiga/town_centers"), 6), Biomes.PLAINS));
        put("village_snowy", gridCustomStructure(StructureFeature.VILLAGE, new StructurePoolFeatureConfig(new Identifier("village/snowy/town_centers"), 6), Biomes.PLAINS));
        put("nether_fossil", ((StructureFeatureInterface)StructureFeature.NETHER_FOSSIL)::gridAnywhere);
        put("bastion_remnant", gridCustomStructure(StructureFeature.BASTION_REMNANT, new BastionRemnantFeatureConfig(BastionRemnantGenerator.START_POOLS_TO_SIZES), Biomes.NETHER_WASTES));
        put("bastion_remnant_housing", gridCustomStructure(StructureFeature.BASTION_REMNANT, new BastionRemnantFeatureConfig(new ImmutableMap.Builder<String,Integer>().put("bastion/units/base", 60).build()), Biomes.NETHER_WASTES));
        put("bastion_remnant_stable", gridCustomStructure(StructureFeature.BASTION_REMNANT, new BastionRemnantFeatureConfig(new ImmutableMap.Builder<String,Integer>().put("bastion/hoglin_stable/origin", 60).build()), Biomes.NETHER_WASTES));
        put("bastion_remnant_treasure", gridCustomStructure(StructureFeature.BASTION_REMNANT, new BastionRemnantFeatureConfig(new ImmutableMap.Builder<String,Integer>().put("bastion/treasure/starters", 60).build()), Biomes.NETHER_WASTES));
        put("bastion_remnant_bridge", gridCustomStructure(StructureFeature.BASTION_REMNANT, new BastionRemnantFeatureConfig(new ImmutableMap.Builder<String,Integer>().put("bastion/bridge/start", 60).build()), Biomes.NETHER_WASTES));
    }};

    private static final Map<String, Thing> featureMap = new HashMap<String, Thing>() {{


        put("oak", simpleTree(OAK_TREE_CONFIG));
        put("oak_beehive", simpleTree(
                (OAK_TREE_CONFIG.setTreeDecorators((ImmutableList.of(new BeehiveTreeDecorator(1.0F))))
        )));
        put("oak_large", simpleTree(DefaultBiomeFeatures.FANCY_TREE_CONFIG));
        put("oak_large_beehive", simpleTree(
                FANCY_TREE_CONFIG.setTreeDecorators(ImmutableList.of(new BeehiveTreeDecorator(1.0F)))));
        put("birch", simpleTree(DefaultBiomeFeatures.BIRCH_TREE_CONFIG));
        put("birch_large", simpleTree(DefaultBiomeFeatures.LARGE_BIRCH_TREE_CONFIG));
        put("shrub", simpleTree(DefaultBiomeFeatures.JUNGLE_GROUND_BUSH_CONFIG));
        put("jungle", simpleTree(DefaultBiomeFeatures.JUNGLE_TREE_CONFIG));
        put("jungle_large", simpleTree(DefaultBiomeFeatures.MEGA_JUNGLE_TREE_CONFIG));
        put("spruce", simpleTree(DefaultBiomeFeatures.SPRUCE_TREE_CONFIG));
        put("spruce_large", simpleTree(DefaultBiomeFeatures.MEGA_SPRUCE_TREE_CONFIG));
        put("pine", simpleTree(DefaultBiomeFeatures.PINE_TREE_CONFIG));
        put("pine_large", simpleTree(DefaultBiomeFeatures.MEGA_PINE_TREE_CONFIG));
        put("dark_oak", simpleTree(DefaultBiomeFeatures.DARK_OAK_TREE_CONFIG));
        put("acacia", simpleTree(DefaultBiomeFeatures.ACACIA_TREE_CONFIG));
        put("oak_swamp", simpleTree(DefaultBiomeFeatures.SWAMP_TREE_CONFIG));
        put("well", simplePlop(Feature.DESERT_WELL));
        put("grass", simplePatch(DefaultBiomeFeatures.GRASS_CONFIG));
        put("tall_grass", simplePatch(DefaultBiomeFeatures.TALL_GRASS_CONFIG));

        put("lush_grass", simplePatch(DefaultBiomeFeatures.LUSH_GRASS_CONFIG));
        put("fern",  simplePatch(DefaultBiomeFeatures.LARGE_FERN_CONFIG));


        put("cactus", simplePatch(DefaultBiomeFeatures.CACTUS_CONFIG));
        put("dead_bush", simplePatch(DefaultBiomeFeatures.DEAD_BUSH_CONFIG));
        put("fossils", simplePlop(Feature.FOSSIL)); // spawn above, spawn invisible
        put("mushroom_brown", simplePlop(Feature.HUGE_BROWN_MUSHROOM.configure(DefaultBiomeFeatures.HUGE_BROWN_MUSHROOM_CONFIG)));
        put("mushroom_red", simplePlop(Feature.HUGE_RED_MUSHROOM.configure(DefaultBiomeFeatures.HUGE_RED_MUSHROOM_CONFIG)));
        put("ice_spike", simplePlop(Feature.ICE_SPIKE));
        put("glowstone", simplePlop(Feature.GLOWSTONE_BLOB));
        put("melon", simplePatch(DefaultBiomeFeatures.MELON_PATCH_CONFIG));
        put("melon_pile", simplePlop(Feature.BLOCK_PILE.configure(DefaultBiomeFeatures.MELON_PILE_CONFIG)));
        put("pumpkin", simplePatch(DefaultBiomeFeatures.PUMPKIN_PATCH_CONFIG));
        put("pumpkin_pile", simplePlop(Feature.BLOCK_PILE.configure(DefaultBiomeFeatures.PUMPKIN_PILE_CONFIG)));
        put("sugarcane", simplePatch(DefaultBiomeFeatures.SUGAR_CANE_CONFIG));
        put("lilypad", simplePatch(DefaultBiomeFeatures.LILY_PAD_CONFIG));
        put("dungeon", simplePlop(Feature.MONSTER_ROOM));
        put("iceberg", simplePlop(Feature.ICEBERG.configure(new SingleStateFeatureConfig(Blocks.PACKED_ICE.getDefaultState()))));
        put("iceberg_blue", simplePlop(Feature.ICEBERG.configure(new SingleStateFeatureConfig(Blocks.BLUE_ICE.getDefaultState()))));
        put("lake", simplePlop(Feature.LAKE.configure(new SingleStateFeatureConfig(Blocks.WATER.getDefaultState()))));
        put("lake_lava", simplePlop(Feature.LAKE.configure(new SingleStateFeatureConfig(Blocks.LAVA.getDefaultState()))));
        //put("end tower", simplePlop(Feature.END_SPIKE.configure(new EndSpikeFeatureConfig(false, ))));
        put("end_island", simplePlop(Feature.END_ISLAND));
        put("chorus", simplePlop(Feature.CHORUS_PLANT));
        put("sea_grass", simplePlop(Feature.SEAGRASS.configure( new SeagrassFeatureConfig(80, 0.8D))));
        put("sea_grass_river", simplePlop(Feature.SEAGRASS.configure( new SeagrassFeatureConfig(48, 0.4D))));
        put("kelp", simplePlop(Feature.KELP));
        put("coral_tree", simplePlop(Feature.CORAL_TREE));
        put("coral_mushroom", simplePlop(Feature.CORAL_MUSHROOM));
        put("coral_claw", simplePlop(Feature.CORAL_CLAW));
        put("coral", (w, p) ->
                w.random.nextInt(3)!=0?
                        (w.random.nextInt(2)!=0?
                                simplePlop(Feature.CORAL_MUSHROOM).plop(w, p):
                                simplePlop(Feature.CORAL_TREE).plop(w, p))
                        :simplePlop(Feature.CORAL_CLAW).plop(w, p));
        put("sea_pickle", simplePlop(Feature.SEA_PICKLE.configure( new SeaPickleFeatureConfig(20))));
        put("boulder", simplePlop(Feature.FOREST_ROCK.configure( new BoulderFeatureConfig(Blocks.MOSSY_COBBLESTONE.getDefaultState(), 0))));
        put("crimson_fungus", simplePlop(Feature.HUGE_FUNGUS.configure(HugeFungusFeatureConfig.CRIMSON_FUNGUS_NOT_PLANTED_CONFIG)));
        put("warped_fungus", simplePlop(Feature.HUGE_FUNGUS.configure(HugeFungusFeatureConfig.WARPED_FUNGUS_NOT_PLANTED_CONFIG)));
        put("nether_sprouts", simplePlop(Feature.NETHER_FOREST_VEGETATION.configure(DefaultBiomeFeatures.NETHER_SPROUTS_CONFIG)));
        put("crimson_roots", simplePlop(Feature.NETHER_FOREST_VEGETATION.configure(DefaultBiomeFeatures.CRIMSON_ROOTS_CONFIG)));
        put("warped_roots", simplePlop(Feature.NETHER_FOREST_VEGETATION.configure(DefaultBiomeFeatures.WARPED_ROOTS_CONFIG)));
        put("weeping_vines", simplePlop(Feature.WEEPING_VINES));
        put("twisting_vines", simplePlop(Feature.TWISTING_VINES));
        put("basalt_pillar", simplePlop(Feature.BASALT_PILLAR));

        //structures
        put("monument",  ((StructureFeatureInterface)StructureFeature.MONUMENT)::plopAnywhere);
        put("fortress", ((StructureFeatureInterface)StructureFeature.FORTRESS)::plopAnywhere);
        put("mansion", ((StructureFeatureInterface)StructureFeature.MANSION)::plopAnywhere);
        put("jungle_temple", ((StructureFeatureInterface)StructureFeature.JUNGLE_PYRAMID)::plopAnywhere);
        put("desert_temple", ((StructureFeatureInterface)StructureFeature.DESERT_PYRAMID)::plopAnywhere);
        put("end_city", ((StructureFeatureInterface)StructureFeature.END_CITY)::plopAnywhere);
        put("igloo", ((StructureFeatureInterface)StructureFeature.IGLOO)::plopAnywhere);
        put("shipwreck", spawnCustomStructure(StructureFeature.SHIPWRECK, new ShipwreckFeatureConfig(true), Biomes.PLAINS));
        put("shipwreck2", spawnCustomStructure(StructureFeature.SHIPWRECK, new ShipwreckFeatureConfig(false), Biomes.PLAINS));
        put("witch_hut", ((StructureFeatureInterface)StructureFeature.field_24851)::plopAnywhere);
        put("stronghold", ((StructureFeatureInterface)StructureFeature.STRONGHOLD)::plopAnywhere);

        put("ocean_ruin_small", spawnCustomStructure(StructureFeature.OCEAN_RUIN,
                new OceanRuinFeatureConfig(OceanRuinFeature.BiomeType.COLD, 0.0F, 0.5F), Biomes.PLAINS));
        put("ocean_ruin_warm_small", spawnCustomStructure(StructureFeature.OCEAN_RUIN,
                new OceanRuinFeatureConfig(OceanRuinFeature.BiomeType.WARM, 0.0F, 0.5F), Biomes.PLAINS));
        put("ocean_ruin_tall", spawnCustomStructure(StructureFeature.OCEAN_RUIN,
                new OceanRuinFeatureConfig(OceanRuinFeature.BiomeType.COLD, 1.0F, 0.0F), Biomes.PLAINS));
        put("ocean_ruin_warm_tall", spawnCustomStructure(StructureFeature.OCEAN_RUIN,
                new OceanRuinFeatureConfig(OceanRuinFeature.BiomeType.WARM, 1.0F, 0.0F), Biomes.PLAINS));
        put("ocean_ruin", spawnCustomStructure(StructureFeature.OCEAN_RUIN,
                new OceanRuinFeatureConfig(OceanRuinFeature.BiomeType.COLD, 1.0F, 1.0F), Biomes.PLAINS));
        put("ocean_ruin_warm", spawnCustomStructure(StructureFeature.OCEAN_RUIN,
                new OceanRuinFeatureConfig(OceanRuinFeature.BiomeType.WARM, 1.0F, 1.0F), Biomes.PLAINS));
        put("treasure", ((StructureFeatureInterface)StructureFeature.BURIED_TREASURE)::plopAnywhere);

        put("pillager_outpost", ((StructureFeatureInterface)StructureFeature.PILLAGER_OUTPOST)::plopAnywhere);


        put("mineshaft", spawnCustomStructure(StructureFeature.MINESHAFT, new MineshaftFeatureConfig(0.0, MineshaftFeature.Type.NORMAL), Biomes.PLAINS));
        put("mineshaft_mesa", spawnCustomStructure(StructureFeature.MINESHAFT, new MineshaftFeatureConfig(0.0, MineshaftFeature.Type.MESA), Biomes.PLAINS));

        put("village", spawnCustomStructure(StructureFeature.VILLAGE, new StructurePoolFeatureConfig(new Identifier("village/plains/town_centers"),6), Biomes.PLAINS));
        put("village_desert", spawnCustomStructure(StructureFeature.VILLAGE, new StructurePoolFeatureConfig(new Identifier("village/desert/town_centers"), 6), Biomes.PLAINS));
        put("village_savanna", spawnCustomStructure(StructureFeature.VILLAGE, new StructurePoolFeatureConfig(new Identifier("village/savanna/town_centers"), 6), Biomes.PLAINS));
        put("village_taiga", spawnCustomStructure(StructureFeature.VILLAGE, new StructurePoolFeatureConfig(new Identifier("village/taiga/town_centers"), 6), Biomes.PLAINS));
        put("village_snowy", spawnCustomStructure(StructureFeature.VILLAGE, new StructurePoolFeatureConfig(new Identifier("village/snowy/town_centers"), 6), Biomes.PLAINS));
        put("nether_fossil", ((StructureFeatureInterface)StructureFeature.NETHER_FOSSIL)::plopAnywhere);
        put("bastion_remnant", spawnCustomStructure(StructureFeature.BASTION_REMNANT, new BastionRemnantFeatureConfig(BastionRemnantGenerator.START_POOLS_TO_SIZES), Biomes.NETHER_WASTES));
        put("bastion_remnant_housing", spawnCustomStructure(StructureFeature.BASTION_REMNANT, new BastionRemnantFeatureConfig(new ImmutableMap.Builder<String,Integer>().put("bastion/units/base", 60).build()), Biomes.NETHER_WASTES));
        put("bastion_remnant_stable", spawnCustomStructure(StructureFeature.BASTION_REMNANT, new BastionRemnantFeatureConfig(new ImmutableMap.Builder<String,Integer>().put("bastion/hoglin_stable/origin", 60).build()), Biomes.NETHER_WASTES));
        put("bastion_remnant_treasure", spawnCustomStructure(StructureFeature.BASTION_REMNANT, new BastionRemnantFeatureConfig(new ImmutableMap.Builder<String,Integer>().put("bastion/treasure/starters", 60).build()), Biomes.NETHER_WASTES));
        put("bastion_remnant_bridge", spawnCustomStructure(StructureFeature.BASTION_REMNANT, new BastionRemnantFeatureConfig(new ImmutableMap.Builder<String,Integer>().put("bastion/bridge/start", 60).build()), Biomes.NETHER_WASTES));
        put("ruined_portal", ((StructureFeatureInterface)StructureFeature.RUINED_PORTAL)::plopAnywhere);

    }};

}
