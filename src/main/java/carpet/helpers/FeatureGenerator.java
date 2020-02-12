package carpet.helpers;

import carpet.CarpetSettings;
import carpet.fakes.StructureFeatureInterface;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.biome.DefaultBiomeFeatures;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.VanillaLayeredBiomeSource;
import net.minecraft.world.biome.source.VanillaLayeredBiomeSourceConfig;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.OverworldChunkGenerator;
import net.minecraft.world.gen.chunk.OverworldChunkGeneratorConfig;
import net.minecraft.world.gen.decorator.BeehiveTreeDecorator;
import net.minecraft.world.gen.feature.BoulderFeatureConfig;
import net.minecraft.world.gen.feature.BranchedTreeFeatureConfig;
import net.minecraft.world.gen.feature.SingleStateFeatureConfig;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.MineshaftFeature;
import net.minecraft.world.gen.feature.MineshaftFeatureConfig;
import net.minecraft.world.gen.feature.OceanRuinFeature;
import net.minecraft.world.gen.feature.OceanRuinFeatureConfig;
import net.minecraft.world.gen.feature.RandomPatchFeatureConfig;
import net.minecraft.world.gen.feature.SeaPickleFeatureConfig;
import net.minecraft.world.gen.feature.SeagrassFeatureConfig;
import net.minecraft.world.gen.feature.ShipwreckFeatureConfig;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.gen.feature.VillageFeatureConfig;
import net.minecraft.world.gen.foliage.BlobFoliagePlacer;
import net.minecraft.world.gen.stateprovider.SimpleBlockStateProvider;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FeatureGenerator
{
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
                return feature.generate(w, w.getChunkManager().getChunkGenerator(), w.random, p);
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

    private static Thing simpleTree(BranchedTreeFeatureConfig config)
    {
        return simplePlop(Feature.NORMAL_TREE.configure(config));
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
        //if (1+2==3)
        //    throw new RuntimeException("rebuild me");
        return (w, p) -> {
            ChunkGenerator chunkgen = new OverworldChunkGenerator(w, w.getChunkManager().getChunkGenerator().getBiomeSource(), new OverworldChunkGeneratorConfig()) //  BiomeSourceType.VANILLA_LAYERED.applyConfig((BiomeSourceType.VANILLA_LAYERED.getConfig())), ChunkGeneratorType.SURFACE.createSettings())
            {
                @Override
                public <C extends FeatureConfig> C getStructureConfig(Biome biome_1, StructureFeature<C> structureFeature_1)
                {
                    return (C)conf;
                }

                @Override
                public BiomeSource getBiomeSource()
                {
                    return new VanillaLayeredBiomeSource(new VanillaLayeredBiomeSourceConfig(w.getLevelProperties()))
                    {
                        @Override
                        public Biome getBiomeForNoiseGen(int i, int j, int k)
                        {
                            return biome;
                        }

                        @Override
                        public Set<Biome> getBiomesInArea(int int_1, int int_2, int int_3, int int_4)
                        {
                            return Sets.newHashSet(biome);
                        }
                    };
                }
            };


            return ((StructureFeatureInterface)structure).plopAnywhere(w, p, chunkgen, wireOnly);
        };
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


    public static final Map<String, List<String>> structureToFeature = new HashMap<>();
    public static final Map<String, String> featureToStructure = new HashMap<>();
    static
    {
        structureToFeature.put("Monument", Collections.singletonList("monument"));
        structureToFeature.put("EndCity", Collections.singletonList("end_city"));
        structureToFeature.put("Ocean_Ruin", Arrays.asList("ocean_ruin", "ocean_ruin_warm", "ocean_ruin_small", "ocean_ruin_warm_small", "ocean_ruin_tall", "ocean_ruin_warm_tall"));
        structureToFeature.put("Village", Arrays.asList("village", "village_desert", "village_savanna", "village_taiga", "village_snowy"));
        structureToFeature.put("Mansion", Collections.singletonList("mansion"));
        structureToFeature.put("Buried_Treasure", Collections.singletonList("treasure"));
        structureToFeature.put("Swamp_Hut", Collections.singletonList("witch_hut"));
        structureToFeature.put("Stronghold", Collections.singletonList("stronghold"));
        structureToFeature.put("Desert_Pyramid", Collections.singletonList("desert_temple"));
        structureToFeature.put("Jungle_Pyramid", Collections.singletonList("jungle_temple"));
        structureToFeature.put("Shipwreck", Arrays.asList("shipwreck", "shipwreck2"));
        structureToFeature.put("Pillager_Outpost", Collections.singletonList("pillager_outpost"));
        structureToFeature.put("Mineshaft", Arrays.asList("mineshaft", "mineshaft_mesa"));
        structureToFeature.put("Igloo", Collections.singletonList("igloo"));
        structureToFeature.put("Fortress", Collections.singletonList("fortress"));

        structureToFeature.forEach((key, value) -> value.forEach(el -> featureToStructure.put(el, key)));
    }


    private static final Map<String, Thing> gridMap = new HashMap<String, Thing>() {{
        put("monument",  ((StructureFeatureInterface)Feature.OCEAN_MONUMENT)::gridAnywhere);
        put("fortress", ((StructureFeatureInterface)Feature.NETHER_BRIDGE)::gridAnywhere);
        put("mansion", ((StructureFeatureInterface)Feature.WOODLAND_MANSION)::gridAnywhere);
        put("jungle_temple", ((StructureFeatureInterface)Feature.JUNGLE_TEMPLE)::gridAnywhere);
        put("desert_temple", ((StructureFeatureInterface)Feature.DESERT_PYRAMID)::gridAnywhere);
        put("end_city", ((StructureFeatureInterface)Feature.END_CITY)::gridAnywhere);
        put("igloo", ((StructureFeatureInterface)Feature.IGLOO)::gridAnywhere);
        put("shipwreck", gridCustomStructure(Feature.SHIPWRECK, new ShipwreckFeatureConfig(true), Biomes.PLAINS));
        put("shipwreck2", gridCustomStructure(Feature.SHIPWRECK, new ShipwreckFeatureConfig(false), Biomes.PLAINS));
        put("witch_hut", ((StructureFeatureInterface)Feature.SWAMP_HUT)::gridAnywhere);
        put("stronghold", ((StructureFeatureInterface)Feature.STRONGHOLD)::gridAnywhere);

        put("ocean_ruin_small", gridCustomStructure(Feature.OCEAN_RUIN,
                new OceanRuinFeatureConfig(OceanRuinFeature.BiomeType.COLD, 0.0F, 0.5F), Biomes.PLAINS));
        put("ocean_ruin_warm_small", gridCustomStructure(Feature.OCEAN_RUIN,
                new OceanRuinFeatureConfig(OceanRuinFeature.BiomeType.WARM, 0.0F, 0.5F), Biomes.PLAINS));
        put("ocean_ruin_tall", gridCustomStructure(Feature.OCEAN_RUIN,
                new OceanRuinFeatureConfig(OceanRuinFeature.BiomeType.COLD, 1.0F, 0.0F), Biomes.PLAINS));
        put("ocean_ruin_warm_tall", gridCustomStructure(Feature.OCEAN_RUIN,
                new OceanRuinFeatureConfig(OceanRuinFeature.BiomeType.WARM, 1.0F, 0.0F), Biomes.PLAINS));
        put("ocean_ruin", gridCustomStructure(Feature.OCEAN_RUIN,
                new OceanRuinFeatureConfig(OceanRuinFeature.BiomeType.COLD, 1.0F, 1.0F), Biomes.PLAINS));
        put("ocean_ruin_warm", gridCustomStructure(Feature.OCEAN_RUIN,
                new OceanRuinFeatureConfig(OceanRuinFeature.BiomeType.WARM, 1.0F, 1.0F), Biomes.PLAINS));
        put("treasure", ((StructureFeatureInterface)Feature.BURIED_TREASURE)::gridAnywhere);

        put("pillager_outpost", ((StructureFeatureInterface)Feature.PILLAGER_OUTPOST)::gridAnywhere);


        put("mineshaft", gridCustomStructure(Feature.MINESHAFT, new MineshaftFeatureConfig(0.0, MineshaftFeature.Type.NORMAL), Biomes.PLAINS));
        put("mineshaft_mesa", gridCustomStructure(Feature.MINESHAFT, new MineshaftFeatureConfig(0.0, MineshaftFeature.Type.MESA), Biomes.PLAINS));

        put("village", gridCustomStructure(Feature.VILLAGE, new VillageFeatureConfig("village/plains/town_centers",6), Biomes.PLAINS));
        put("village_desert", gridCustomStructure(Feature.VILLAGE, new VillageFeatureConfig("village/desert/town_centers", 6), Biomes.PLAINS));
        put("village_savanna", gridCustomStructure(Feature.VILLAGE, new VillageFeatureConfig("village/savanna/town_centers", 6), Biomes.PLAINS));
        put("village_taiga", gridCustomStructure(Feature.VILLAGE, new VillageFeatureConfig("village/taiga/town_centers", 6), Biomes.PLAINS));
        put("village_snowy", gridCustomStructure(Feature.VILLAGE, new VillageFeatureConfig("village/snowy/town_centers", 6), Biomes.PLAINS));

    }};

    private static final Map<String, Thing> featureMap = new HashMap<String, Thing>() {{


        put("oak", simpleTree(DefaultBiomeFeatures.OAK_TREE_CONFIG));
        put("oak_beehive", simpleTree(
                (new BranchedTreeFeatureConfig.Builder(
                        new SimpleBlockStateProvider(Blocks.OAK_LOG.getDefaultState()),
                        new SimpleBlockStateProvider(Blocks.OAK_LEAVES.getDefaultState()),
                        new BlobFoliagePlacer(2, 0))
                ).baseHeight(4).heightRandA(2).foliageHeight(3).noVines()
                .treeDecorators(ImmutableList.of(new BeehiveTreeDecorator(1.0F))).build()
        ));
        put("oak_large", simplePlop(Feature.FANCY_TREE.configure(DefaultBiomeFeatures.FANCY_TREE_CONFIG)));
        put("oak_large_beehive", simplePlop(Feature.FANCY_TREE.configure(
                (new BranchedTreeFeatureConfig.Builder(
                        new SimpleBlockStateProvider(Blocks.OAK_LOG.getDefaultState()),
                        new SimpleBlockStateProvider(Blocks.OAK_LEAVES.getDefaultState()),
                        new BlobFoliagePlacer(0, 0))
                ).treeDecorators(ImmutableList.of(new BeehiveTreeDecorator(1.0F))).build()
        )));
        put("birch", simpleTree(DefaultBiomeFeatures.BIRCH_TREE_CONFIG));
        put("birch_large", simpleTree(DefaultBiomeFeatures.LARGE_BIRCH_TREE_CONFIG));
        put("shrub", simplePlop(Feature.JUNGLE_GROUND_BUSH.configure(DefaultBiomeFeatures.JUNGLE_GROUND_BUSH_CONFIG)));
        put("jungle", simpleTree(DefaultBiomeFeatures.JUNGLE_TREE_CONFIG));
        put("jungle_large", simplePlop(Feature.MEGA_JUNGLE_TREE.configure(DefaultBiomeFeatures.MEGA_JUNGLE_TREE_CONFIG)));
        put("spruce", simpleTree(DefaultBiomeFeatures.SPRUCE_TREE_CONFIG));
        put("spruce_large", simplePlop(Feature.MEGA_SPRUCE_TREE.configure(DefaultBiomeFeatures.MEGA_SPRUCE_TREE_CONFIG)));
        put("pine", simpleTree(DefaultBiomeFeatures.PINE_TREE_CONFIG));
        put("pine_large", simplePlop(Feature.MEGA_SPRUCE_TREE.configure(DefaultBiomeFeatures.MEGA_PINE_TREE_CONFIG)));
        put("dark_oak", simplePlop(Feature.DARK_OAK_TREE.configure(DefaultBiomeFeatures.DARK_OAK_TREE_CONFIG)));
        put("acacia", simplePlop(Feature.ACACIA_TREE.configure(DefaultBiomeFeatures.ACACIA_TREE_CONFIG)));
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
        //structures
        put("monument",  ((StructureFeatureInterface)Feature.OCEAN_MONUMENT)::plopAnywhere);
        put("fortress", ((StructureFeatureInterface)Feature.NETHER_BRIDGE)::plopAnywhere);
        put("mansion", ((StructureFeatureInterface)Feature.WOODLAND_MANSION)::plopAnywhere);
        put("jungle_temple", ((StructureFeatureInterface)Feature.JUNGLE_TEMPLE)::plopAnywhere);
        put("desert_temple", ((StructureFeatureInterface)Feature.DESERT_PYRAMID)::plopAnywhere);
        put("end_city", ((StructureFeatureInterface)Feature.END_CITY)::plopAnywhere);
        put("igloo", ((StructureFeatureInterface)Feature.IGLOO)::plopAnywhere);
        put("shipwreck", spawnCustomStructure(Feature.SHIPWRECK, new ShipwreckFeatureConfig(true), Biomes.PLAINS));
        put("shipwreck2", spawnCustomStructure(Feature.SHIPWRECK, new ShipwreckFeatureConfig(false), Biomes.PLAINS));
        put("witch_hut", ((StructureFeatureInterface)Feature.SWAMP_HUT)::plopAnywhere);
        put("stronghold", ((StructureFeatureInterface)Feature.STRONGHOLD)::plopAnywhere);

        put("ocean_ruin_small", spawnCustomStructure(Feature.OCEAN_RUIN,
                new OceanRuinFeatureConfig(OceanRuinFeature.BiomeType.COLD, 0.0F, 0.5F), Biomes.PLAINS));
        put("ocean_ruin_warm_small", spawnCustomStructure(Feature.OCEAN_RUIN,
                new OceanRuinFeatureConfig(OceanRuinFeature.BiomeType.WARM, 0.0F, 0.5F), Biomes.PLAINS));
        put("ocean_ruin_tall", spawnCustomStructure(Feature.OCEAN_RUIN,
                new OceanRuinFeatureConfig(OceanRuinFeature.BiomeType.COLD, 1.0F, 0.0F), Biomes.PLAINS));
        put("ocean_ruin_warm_tall", spawnCustomStructure(Feature.OCEAN_RUIN,
                new OceanRuinFeatureConfig(OceanRuinFeature.BiomeType.WARM, 1.0F, 0.0F), Biomes.PLAINS));
        put("ocean_ruin", spawnCustomStructure(Feature.OCEAN_RUIN,
                new OceanRuinFeatureConfig(OceanRuinFeature.BiomeType.COLD, 1.0F, 1.0F), Biomes.PLAINS));
        put("ocean_ruin_warm", spawnCustomStructure(Feature.OCEAN_RUIN,
                new OceanRuinFeatureConfig(OceanRuinFeature.BiomeType.WARM, 1.0F, 1.0F), Biomes.PLAINS));
        put("treasure", ((StructureFeatureInterface)Feature.BURIED_TREASURE)::plopAnywhere);

        put("pillager_outpost", ((StructureFeatureInterface)Feature.PILLAGER_OUTPOST)::plopAnywhere);


        put("mineshaft", spawnCustomStructure(Feature.MINESHAFT, new MineshaftFeatureConfig(0.0, MineshaftFeature.Type.NORMAL), Biomes.PLAINS));
        put("mineshaft_mesa", spawnCustomStructure(Feature.MINESHAFT, new MineshaftFeatureConfig(0.0, MineshaftFeature.Type.MESA), Biomes.PLAINS));

        put("village", spawnCustomStructure(Feature.VILLAGE, new VillageFeatureConfig("village/plains/town_centers",6), Biomes.PLAINS));
        put("village_desert", spawnCustomStructure(Feature.VILLAGE, new VillageFeatureConfig("village/desert/town_centers", 6), Biomes.PLAINS));
        put("village_savanna", spawnCustomStructure(Feature.VILLAGE, new VillageFeatureConfig("village/savanna/town_centers", 6), Biomes.PLAINS));
        put("village_taiga", spawnCustomStructure(Feature.VILLAGE, new VillageFeatureConfig("village/taiga/town_centers", 6), Biomes.PLAINS));
        put("village_snowy", spawnCustomStructure(Feature.VILLAGE, new VillageFeatureConfig("village/snowy/town_centers", 6), Biomes.PLAINS));
    }};

}
