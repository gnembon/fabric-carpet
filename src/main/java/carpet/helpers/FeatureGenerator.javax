package carpet.helpers;

import carpet.CarpetSettings;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.biome.provider.OverworldBiomeProvider;
import net.minecraft.world.biome.provider.OverworldBiomeProviderSettings;
import net.minecraft.world.gen.ChunkGeneratorOverworld;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.OverworldGenSettings;
import net.minecraft.world.gen.feature.BlockBlobConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.IFeatureConfig;
import net.minecraft.world.gen.feature.IcebergConfig;
import net.minecraft.world.gen.feature.LakesConfig;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import net.minecraft.world.gen.feature.SeaGrassConfig;
import net.minecraft.world.gen.feature.ShrubFeature;
import net.minecraft.world.gen.feature.TallGrassConfig;
import net.minecraft.world.gen.feature.TallGrassFeature;
import net.minecraft.world.gen.feature.structure.MineshaftConfig;
import net.minecraft.world.gen.feature.structure.MineshaftStructure;
import net.minecraft.world.gen.feature.structure.OceanRuinConfig;
import net.minecraft.world.gen.feature.structure.OceanRuinStructure;
import net.minecraft.world.gen.feature.structure.ShipwreckConfig;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.VillageConfig;
import net.minecraft.world.gen.feature.structure.VillagePieces;
import net.minecraft.world.gen.placement.CountConfig;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class FeatureGenerator
{
    @FunctionalInterface
    private interface Thing
    {
        Boolean plop(World world, BlockPos pos);
    }
    private static Thing simplePlop(Feature<NoFeatureConfig> feature)
    {
        return simplePlop(feature, IFeatureConfig.NO_FEATURE_CONFIG);
    }
    private static Thing simplePlop(Feature feature, IFeatureConfig config)
    {
        return (w, p) -> {
            CarpetSettings.skipGenerationChecks=true;
            boolean res = feature.place(w, w.getChunkProvider().getChunkGenerator(), w.rand, p, config);
            CarpetSettings.skipGenerationChecks=false;
            return res;
        };

    }

    private static Thing spawnCustomStructure(Structure structure, IFeatureConfig config)
    {
        return (w, p) -> {
            IChunkGenerator chunkgen = new ChunkGeneratorOverworld(w, null, new OverworldGenSettings())
            {
                @Nullable
                @Override
                public IFeatureConfig getStructureConfig(Biome biomeIn, Structure<? extends IFeatureConfig> structureIn)
                {
                    return config;
                }

                @Override
                public BiomeProvider getBiomeProvider()
                {
                    return new OverworldBiomeProvider(new OverworldBiomeProviderSettings().setWorldInfo(w.getWorldInfo()))
                    {
                        @Nullable
                        @Override
                        public Biome getBiome(BlockPos pos, @Nullable Biome defaultBiome)
                        {
                            return Biomes.PLAINS;
                        }
                    };
                }
            };


            return structure.plopAnywhere(w, p, chunkgen);
        };
    }

    public static Boolean spawn(String name, World world, BlockPos pos)
    {
        if (featureMap.containsKey(name))
            return featureMap.get(name).plop(world, pos);
        return null;
    }

    private static Map<String, Thing> featureMap = new HashMap<String, Thing>() {{


        put("oak", simplePlop(Feature.TREE));
        put("oak_large", simplePlop(Feature.BIG_TREE));
        put("birch", simplePlop(Feature.BIRCH_TREE));
        put("birch_large", simplePlop(Feature.TALL_BIRCH_TREE));
        put("shrub", simplePlop(Feature.SHRUB));
        put("shrub_acacia", simplePlop(new ShrubFeature(Blocks.ACACIA_WOOD.getDefaultState(), Blocks.ACACIA_LEAVES.getDefaultState())));
        put("shrub_birch", simplePlop(new ShrubFeature(Blocks.BIRCH_WOOD.getDefaultState(), Blocks.BIRCH_LEAVES.getDefaultState())));
        put("shrub_snowy", simplePlop(new ShrubFeature(Blocks.BONE_BLOCK.getDefaultState(), Blocks.COBWEB.getDefaultState())));
        put("jungle", simplePlop(Feature.JUNGLE_TREE));
        put("spruce_matchstick", simplePlop(Feature.POINTY_TAIGA_TREE));
        put("dark_oak", simplePlop(Feature.CANOPY_TREE));
        put("acacia", simplePlop(Feature.SAVANNA_TREE));
        put("spruce", simplePlop(Feature.TALL_TAIGA_TREE));
        put("oak_swamp", simplePlop(Feature.SWAMP_TREE));
        put("jungle_large", simplePlop(Feature.MESA_JUNGLE));
        put("spruce_matchstick_large", simplePlop(Feature.MEGA_PINE_TREE_1));
        put("spruce_large", simplePlop(Feature.MEGA_PINE_TREE_2));
        put("well", simplePlop(Feature.DESERT_WELLS));
        put("grass_jungle", simplePlop(Feature.JUNGLE_GRASS));
        put("fern", simplePlop(Feature.TAIGA_GRASS));
        put("grass", simplePlop(new TallGrassFeature(), new TallGrassConfig(Blocks.GRASS.getDefaultState())));
        //put("", simplePlop(Feature.));
        //put("", simplePlop(Feature.));
        //put("", simplePlop(Feature.));
        //put("", simplePlop(Feature.));

        put("cactus", simplePlop(Feature.CACTUS));
        put("dead_bush", simplePlop(Feature.DEAD_BUSH));
        put("fossils", simplePlop(Feature.FOSSILS)); // spawn above, spawn invisible
        put("mushroom_brown", simplePlop(Feature.BIG_BROWN_MUSHROOM));
        put("mushroom_red", simplePlop(Feature.BIG_RED_MUSHROOM));
        put("ice_spike", simplePlop(Feature.ICE_SPIKE));
        put("glowstone", simplePlop(Feature.GLOWSTONE));
        put("melon", simplePlop(Feature.MELON));
        put("pumpkin", simplePlop(Feature.PUMPKIN));
        put("sugarcane", simplePlop(Feature.REED));
        put("lilypad", simplePlop(Feature.WATERLILY));
        put("dungeon", simplePlop(Feature.DUNGEONS));
        put("iceberg", simplePlop(Feature.ICEBERG, new IcebergConfig(Blocks.PACKED_ICE.getDefaultState())));
        put("iceberg_blue", simplePlop(Feature.ICEBERG, new IcebergConfig(Blocks.BLUE_ICE.getDefaultState())));
        put("lake", simplePlop(Feature.LAKES, new LakesConfig(Blocks.WATER)));
        put("lake_lava", simplePlop(Feature.LAKES, new LakesConfig(Blocks.LAVA)));
        //put("end tower", simplePlop(Feature.END_CRYSTAL_TOWER)); // requires more complex setup
        put("end_island", simplePlop(Feature.END_ISLAND));
        put("chorus", simplePlop(Feature.CHORUS_PLANT));
        put("sea_grass", simplePlop(Feature.SEA_GRASS, new SeaGrassConfig(80, 0.8D)));
        put("sea_grass_river", simplePlop(Feature.SEA_GRASS, new SeaGrassConfig(48, 0.4D)));
        put("kelp", simplePlop(Feature.KELP));
        put("coral_tree", simplePlop(Feature.CORAL_TREE));
        put("coral_mushroom", simplePlop(Feature.CORAL_MUSHROOM));
        put("coral_claw", simplePlop(Feature.CORAL_CLAW));
        put("coral", (w, p) ->
                w.rand.nextInt(3)!=0?
                        (w.rand.nextInt(2)!=0?
                                simplePlop(Feature.CORAL_MUSHROOM).plop(w, p):
                                simplePlop(Feature.CORAL_TREE).plop(w, p))
                        :simplePlop(Feature.CORAL_CLAW).plop(w, p));
        put("sea_pickle", simplePlop(Feature.SEA_PICKLE, new CountConfig(20)));
        put("boulder", simplePlop(Feature.BLOCK_BLOB, new BlockBlobConfig(Blocks.MOSSY_COBBLESTONE, 0)));
        //structures
        put("monument",  Feature.OCEAN_MONUMENT::plopAnywhere);
        put("fortress", Feature.FORTRESS::plopAnywhere);
        put("mansion", Feature.WOODLAND_MANSION::plopAnywhere);
        put("jungle_temple", Feature.JUNGLE_PYRAMID::plopAnywhere);
        put("desert_temple", Feature.DESERT_PYRAMID::plopAnywhere);
        put("end_city", Feature.END_CITY::plopAnywhere);
        put("igloo", Feature.IGLOO::plopAnywhere);
        put("shipwreck", spawnCustomStructure(Feature.SHIPWRECK, new ShipwreckConfig(true)));
        put("shipwreck2", spawnCustomStructure(Feature.SHIPWRECK, new ShipwreckConfig(false)));
        put("witchhut", Feature.SWAMP_HUT::plopAnywhere);
        put("stronghold", Feature.STRONGHOLD::plopAnywhere);

        put("oceanruin_small", spawnCustomStructure(Feature.OCEAN_RUIN,
                new OceanRuinConfig(OceanRuinStructure.Type.COLD, 0.0F, 0.5F)));
        put("oceanruin_warm_small", spawnCustomStructure(Feature.OCEAN_RUIN,
                new OceanRuinConfig(OceanRuinStructure.Type.WARM, 0.0F, 0.5F)));
        put("oceanruin_tall", spawnCustomStructure(Feature.OCEAN_RUIN,
                new OceanRuinConfig(OceanRuinStructure.Type.COLD, 1.0F, 0.0F)));
        put("oceanruin_warm_tall", spawnCustomStructure(Feature.OCEAN_RUIN,
                new OceanRuinConfig(OceanRuinStructure.Type.WARM, 1.0F, 0.0F)));
        put("oceanruin", spawnCustomStructure(Feature.OCEAN_RUIN,
                new OceanRuinConfig(OceanRuinStructure.Type.COLD, 1.0F, 1.0F)));
        put("oceanruin_warm", spawnCustomStructure(Feature.OCEAN_RUIN,
                new OceanRuinConfig(OceanRuinStructure.Type.WARM, 1.0F, 1.0F)));
        put("treasure", Feature.BURIED_TREASURE::plopAnywhere);


        put("mineshaft", spawnCustomStructure(Feature.MINESHAFT, new MineshaftConfig(0.0, MineshaftStructure.Type.NORMAL)));
        put("mineshaft_mesa", spawnCustomStructure(Feature.MINESHAFT, new MineshaftConfig(0.0, MineshaftStructure.Type.MESA)));

        put("village", spawnCustomStructure(Feature.VILLAGE, new VillageConfig(0,VillagePieces.Type.OAK)));
        put("village_desert", spawnCustomStructure(Feature.VILLAGE, new VillageConfig(0,VillagePieces.Type.SANDSTONE)));
        put("village_savanna", spawnCustomStructure(Feature.VILLAGE, new VillageConfig(0,VillagePieces.Type.ACACIA)));
        put("village_taiga", spawnCustomStructure(Feature.VILLAGE, new VillageConfig(0,VillagePieces.Type.SPRUCE)));

    }};

}
