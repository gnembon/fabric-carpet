package carpet.helpers;

import carpet.settings.CarpetSettings;
import carpet.fakes.StructureFeatureInterface;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.VanillaLayeredBiomeSource;
import net.minecraft.world.biome.source.VanillaLayeredBiomeSourceConfig;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.OverworldChunkGenerator;
import net.minecraft.world.gen.chunk.OverworldChunkGeneratorConfig;
import net.minecraft.world.gen.feature.BoulderFeatureConfig;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.DoublePlantFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.IcebergFeatureConfig;
import net.minecraft.world.gen.feature.JungleGroundBushFeature;
import net.minecraft.world.gen.feature.LakeFeatureConfig;
import net.minecraft.world.gen.feature.MineshaftFeature;
import net.minecraft.world.gen.feature.MineshaftFeatureConfig;
import net.minecraft.world.gen.feature.OceanRuinFeature;
import net.minecraft.world.gen.feature.OceanRuinFeatureConfig;
import net.minecraft.world.gen.feature.PlantedFeatureConfig;
import net.minecraft.world.gen.feature.SeaPickleFeatureConfig;
import net.minecraft.world.gen.feature.SeagrassFeatureConfig;
import net.minecraft.world.gen.feature.ShipwreckFeatureConfig;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.gen.feature.VillageFeatureConfig;

import java.util.HashMap;
import java.util.Map;

public class FeatureGenerator
{

    @FunctionalInterface
    private interface Thing
    {
        Boolean plop(World world, BlockPos pos);
    }
    private static Thing simplePlop(Feature<DefaultFeatureConfig> feature)
    {
        return simplePlop(feature, FeatureConfig.DEFAULT);
    }
    private static Thing simplePlop(Feature feature, FeatureConfig config)
    {
        return (w, p) -> {
            CarpetSettings.skipGenerationChecks=true;
            boolean res = feature.generate(w, w.getChunkManager().getChunkGenerator(), w.random, p, config);
            CarpetSettings.skipGenerationChecks=false;
            return res;
        };

    }

    private static Thing spawnCustomStructure(StructureFeature structure, FeatureConfig conf, Biome biome)
    {
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
                    return new VanillaLayeredBiomeSource(new VanillaLayeredBiomeSourceConfig().setLevelProperties(w.getLevelProperties()))
                    {
                        @Override
                        public Biome getBiome(int i, int j)
                        {
                            return biome;
                        }
                    };
                }
            };


            return ((StructureFeatureInterface)structure).plopAnywhere(w, p, chunkgen);
        };
    }

    public static Boolean spawn(String name, World world, BlockPos pos)
    {
        if (featureMap.containsKey(name))
            return featureMap.get(name).plop(world, pos);
        return null;
    }

    private static Map<String, Thing> featureMap = new HashMap<String, Thing>() {{


        put("oak", simplePlop(Feature.NORMAL_TREE));
        put("oak_large", simplePlop(Feature.FANCY_TREE));
        put("birch", simplePlop(Feature.BIRCH_TREE));
        put("birch_large", simplePlop(Feature.SUPER_BIRCH_TREE));
        put("shrub", simplePlop(Feature.JUNGLE_GROUND_BUSH));
        put("shrub_acacia", simplePlop(new JungleGroundBushFeature((d) -> FeatureConfig.DEFAULT, Blocks.ACACIA_WOOD.getDefaultState(), Blocks.ACACIA_LEAVES.getDefaultState())));
        put("shrub_birch", simplePlop(new JungleGroundBushFeature((d) -> FeatureConfig.DEFAULT, Blocks.BIRCH_WOOD.getDefaultState(), Blocks.BIRCH_LEAVES.getDefaultState())));
        put("shrub_snowy", simplePlop(new JungleGroundBushFeature((d) -> FeatureConfig.DEFAULT, Blocks.BONE_BLOCK.getDefaultState(), Blocks.COBWEB.getDefaultState())));
        put("jungle", simplePlop(Feature.JUNGLE_TREE));
        put("spruce_matchstick", simplePlop(Feature.PINE_TREE));
        put("dark_oak", simplePlop(Feature.DARK_OAK_TREE));
        put("acacia", simplePlop(Feature.SAVANNA_TREE));
        put("spruce", simplePlop(Feature.SPRUCE_TREE));
        put("oak_swamp", simplePlop(Feature.SWAMP_TREE));
        put("jungle_large", simplePlop(Feature.MEGA_JUNGLE_TREE));
        //put("spruce_matchstick_large", simplePlop(Feature.MEGA_PINE_TREE));??
        put("spruce_large", simplePlop(Feature.MEGA_SPRUCE_TREE));
        put("well", simplePlop(Feature.DESERT_WELL));
        put("grass_jungle", simplePlop(Feature.JUNGLE_GRASS));
        put("fern", simplePlop(Feature.TAIGA_GRASS));
        put("grass", simplePlop(Feature.DOUBLE_PLANT, new DoublePlantFeatureConfig(Blocks.TALL_GRASS.getDefaultState())));
        //put("", simplePlop(Feature.));
        //put("", simplePlop(Feature.));
        //put("", simplePlop(Feature.));
        //put("", simplePlop(Feature.));

        put("cactus", simplePlop(Feature.CACTUS));
        put("dead_bush", simplePlop(Feature.DEAD_BUSH));
        put("fossils", simplePlop(Feature.FOSSIL)); // spawn above, spawn invisible
        put("mushroom_brown", simplePlop(Feature.HUGE_BROWN_MUSHROOM, new PlantedFeatureConfig(false)));
        put("mushroom_red", simplePlop(Feature.HUGE_RED_MUSHROOM, new PlantedFeatureConfig(false)));
        put("ice_spike", simplePlop(Feature.ICE_SPIKE));
        put("glowstone", simplePlop(Feature.GLOWSTONE_BLOB));
        put("melon", simplePlop(Feature.MELON));
        put("pumpkin", simplePlop(Feature.PUMPKIN));
        put("sugarcane", simplePlop(Feature.REED));
        put("lilypad", simplePlop(Feature.WATERLILY));
        put("dungeon", simplePlop(Feature.MONSTER_ROOM));
        put("iceberg", simplePlop(Feature.ICEBERG, new IcebergFeatureConfig(Blocks.PACKED_ICE.getDefaultState())));
        put("iceberg_blue", simplePlop(Feature.ICEBERG, new IcebergFeatureConfig(Blocks.BLUE_ICE.getDefaultState())));
        put("lake", simplePlop(Feature.LAKE, new LakeFeatureConfig(Blocks.WATER.getDefaultState())));
        put("lake_lava", simplePlop(Feature.LAKE, new LakeFeatureConfig(Blocks.LAVA.getDefaultState())));
        //put("end tower", simplePlop(Feature.END_CRYSTAL_TOWER)); // requires more complex setup
        put("end_island", simplePlop(Feature.END_ISLAND));
        put("chorus", simplePlop(Feature.CHORUS_PLANT));
        put("sea_grass", simplePlop(Feature.SEAGRASS, new SeagrassFeatureConfig(80, 0.8D)));
        put("sea_grass_river", simplePlop(Feature.SEAGRASS, new SeagrassFeatureConfig(48, 0.4D)));
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
        put("sea_pickle", simplePlop(Feature.SEA_PICKLE, new SeaPickleFeatureConfig(20)));
        put("boulder", simplePlop(Feature.FOREST_ROCK, new BoulderFeatureConfig(Blocks.MOSSY_COBBLESTONE.getDefaultState(), 0)));
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
        put("witchhut", ((StructureFeatureInterface)Feature.SWAMP_HUT)::plopAnywhere);
        put("stronghold", ((StructureFeatureInterface)Feature.STRONGHOLD)::plopAnywhere);

        put("oceanruin_small", spawnCustomStructure(Feature.OCEAN_RUIN,
                new OceanRuinFeatureConfig(OceanRuinFeature.BiomeType.COLD, 0.0F, 0.5F), Biomes.PLAINS));
        put("oceanruin_warm_small", spawnCustomStructure(Feature.OCEAN_RUIN,
                new OceanRuinFeatureConfig(OceanRuinFeature.BiomeType.WARM, 0.0F, 0.5F), Biomes.PLAINS));
        put("oceanruin_tall", spawnCustomStructure(Feature.OCEAN_RUIN,
                new OceanRuinFeatureConfig(OceanRuinFeature.BiomeType.COLD, 1.0F, 0.0F), Biomes.PLAINS));
        put("oceanruin_warm_tall", spawnCustomStructure(Feature.OCEAN_RUIN,
                new OceanRuinFeatureConfig(OceanRuinFeature.BiomeType.WARM, 1.0F, 0.0F), Biomes.PLAINS));
        put("oceanruin", spawnCustomStructure(Feature.OCEAN_RUIN,
                new OceanRuinFeatureConfig(OceanRuinFeature.BiomeType.COLD, 1.0F, 1.0F), Biomes.PLAINS));
        put("oceanruin_warm", spawnCustomStructure(Feature.OCEAN_RUIN,
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
