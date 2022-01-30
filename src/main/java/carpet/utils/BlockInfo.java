package carpet.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.BaseComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;

public class BlockInfo
{
    public static final Map<SoundType, String> soundName = new HashMap<SoundType, String>() {{
        put(SoundType.WOOD,   "wood"  );
        put(SoundType.GRAVEL, "gravel");
        put(SoundType.GRASS,  "grass" );
        put(SoundType.LILY_PAD, "lily_pad");
        put(SoundType.STONE,  "stone" );
        put(SoundType.METAL,  "metal" );
        put(SoundType.GLASS , "glass" );
        put(SoundType.WOOL  , "wool"  );
        put(SoundType.SAND  , "sand"  );
        put(SoundType.SNOW  , "snow"  );
        put(SoundType.POWDER_SNOW  , "powder_snow"  );
        put(SoundType.LADDER, "ladder");
        put(SoundType.ANVIL , "anvil" );
        put(SoundType.SLIME_BLOCK  , "slime"  );
        put(SoundType.HONEY_BLOCK  , "honey"  );
        put(SoundType.WET_GRASS , "sea_grass" );
        put(SoundType.CORAL_BLOCK , "coral" );
        put(SoundType.BAMBOO , "bamboo" );
        put(SoundType.BAMBOO_SAPLING , "shoots" );
        put(SoundType.SCAFFOLDING , "scaffolding" );
        put(SoundType.SWEET_BERRY_BUSH , "berry" );
        put(SoundType.CROP , "crop" );
        put(SoundType.HARD_CROP , "stem" );
        put(SoundType.VINE , "vine" );
        put(SoundType.NETHER_WART , "wart" );
        put(SoundType.LANTERN , "lantern" );
        put(SoundType.STEM, "fungi_stem");
        put(SoundType.NYLIUM, "nylium");
        put(SoundType.FUNGUS, "fungus");
        put(SoundType.ROOTS, "roots");
        put(SoundType.SHROOMLIGHT, "shroomlight");
        put(SoundType.WEEPING_VINES, "weeping_vine");
        put(SoundType.TWISTING_VINES, "twisting_vine");
        put(SoundType.SOUL_SAND, "soul_sand");
        put(SoundType.SOUL_SOIL, "soul_soil");
        put(SoundType.BASALT, "basalt");
        put(SoundType.WART_BLOCK, "wart");
        put(SoundType.NETHERRACK, "netherrack");
        put(SoundType.NETHER_BRICKS, "nether_bricks");
        put(SoundType.NETHER_SPROUTS, "nether_sprouts");
        put(SoundType.NETHER_ORE, "nether_ore");
        put(SoundType.BONE_BLOCK, "bone");
        put(SoundType.NETHERITE_BLOCK, "netherite");
        put(SoundType.ANCIENT_DEBRIS, "ancient_debris");
        put(SoundType.LODESTONE, "lodestone");
        put(SoundType.CHAIN, "chain");
        put(SoundType.NETHER_GOLD_ORE, "nether_gold_ore");
        put(SoundType.GILDED_BLACKSTONE, "gilded_blackstone");
        put(SoundType.CANDLE, "candle");
        put(SoundType.AMETHYST, "amethyst");
        put(SoundType.AMETHYST_CLUSTER, "amethyst_cluster");
        put(SoundType.SMALL_AMETHYST_BUD, "small_amethyst_bud");
        put(SoundType.MEDIUM_AMETHYST_BUD, "medium_amethyst_bud");
        put(SoundType.LARGE_AMETHYST_BUD, "large_amethyst_bud");

        put(SoundType.TUFF, "tuff");
        put(SoundType.CALCITE, "calcite");
        put(SoundType.DRIPSTONE_BLOCK, "dripstone");
        put(SoundType.POINTED_DRIPSTONE, "pointed_dripstone");
        put(SoundType.COPPER, "copper");
        put(SoundType.CAVE_VINES, "cave_vine");
        put(SoundType.SPORE_BLOSSOM, "spore_blossom");
        put(SoundType.AZALEA, "azalea");
        put(SoundType.FLOWERING_AZALEA, "flowering_azalea");
        put(SoundType.MOSS_CARPET, "moss_carpet");
        put(SoundType.MOSS, "moss");
        put(SoundType.BIG_DRIPLEAF, "big_dripleaf");
        put(SoundType.SMALL_DRIPLEAF, "small_dripleaf");
        put(SoundType.ROOTED_DIRT, "rooted_dirt");
        put(SoundType.HANGING_ROOTS, "hanging_roots");
        put(SoundType.AZALEA_LEAVES, "azalea_leaves");
        put(SoundType.SCULK_SENSOR, "sculk_sensor");
        put(SoundType.GLOW_LICHEN, "glow_lichen");
        put(SoundType.DEEPSLATE, "deepslate");
        put(SoundType.DEEPSLATE_BRICKS, "deepslate_bricks");
        put(SoundType.DEEPSLATE_TILES, "deepslate_tiles");
        put(SoundType.POLISHED_DEEPSLATE, "polished_deepslate");
    }};

    public static final Map<MaterialColor, String> mapColourName = new HashMap<MaterialColor, String>() {{
        put(MaterialColor.NONE     , "air"       );
        put(MaterialColor.GRASS     , "grass"     );
        put(MaterialColor.SAND       , "sand"      );
        put(MaterialColor.WOOL        , "wool"      );
        put(MaterialColor.FIRE       , "tnt"       );
        put(MaterialColor.ICE        , "ice"       );
        put(MaterialColor.METAL      , "iron"      );
        put(MaterialColor.PLANT    , "foliage"   );
        put(MaterialColor.SNOW     , "snow"      );
        put(MaterialColor.CLAY       , "clay"      );
        put(MaterialColor.DIRT       , "dirt"      );
        put(MaterialColor.STONE      , "stone"     );
        put(MaterialColor.WATER      , "water"     );
        put(MaterialColor.WOOD       , "wood"      );
        put(MaterialColor.QUARTZ     , "quartz"    );
        put(MaterialColor.COLOR_ORANGE    , "adobe"     );
        put(MaterialColor.COLOR_MAGENTA   , "magenta"   );
        put(MaterialColor.COLOR_LIGHT_BLUE, "light_blue");
        put(MaterialColor.COLOR_YELLOW    , "yellow"    );
        put(MaterialColor.COLOR_LIGHT_GREEN      , "lime"      );
        put(MaterialColor.COLOR_PINK      , "pink"      );
        put(MaterialColor.COLOR_GRAY      , "gray"      );
        put(MaterialColor.COLOR_LIGHT_GRAY, "light_gray");
        put(MaterialColor.COLOR_CYAN      , "cyan"      );
        put(MaterialColor.COLOR_PURPLE    , "purple"    );
        put(MaterialColor.COLOR_BLUE      , "blue"      );
        put(MaterialColor.COLOR_BROWN     , "brown"     );
        put(MaterialColor.COLOR_GREEN     , "green"     );
        put(MaterialColor.COLOR_RED       , "red"       );
        put(MaterialColor.COLOR_BLACK     , "black"     );
        put(MaterialColor.GOLD      , "gold"      );
        put(MaterialColor.DIAMOND    , "diamond"   );
        put(MaterialColor.LAPIS      , "lapis"     );
        put(MaterialColor.EMERALD    , "emerald"   );
        put(MaterialColor.PODZOL     , "obsidian"  );
        put(MaterialColor.NETHER     , "netherrack"); //TODO fix these
        put(MaterialColor.TERRACOTTA_WHITE      , "white_terracotta"      );
        put(MaterialColor.TERRACOTTA_ORANGE    , "orange_terracotta"     );
        put(MaterialColor.TERRACOTTA_MAGENTA   , "magenta_terracotta"    );
        put(MaterialColor.TERRACOTTA_LIGHT_BLUE, "light_blue_terracotta" );
        put(MaterialColor.TERRACOTTA_YELLOW    , "yellow_terracotta"     );
        put(MaterialColor.TERRACOTTA_LIGHT_GREEN      , "lime_terracotta"       );
        put(MaterialColor.TERRACOTTA_PINK      , "pink_terracotta"       );
        put(MaterialColor.TERRACOTTA_GRAY      , "gray_terracotta"       );
        put(MaterialColor.TERRACOTTA_LIGHT_GRAY, "light_gray_terracotta" );
        put(MaterialColor.TERRACOTTA_CYAN      , "cyan_terracotta"       );
        put(MaterialColor.TERRACOTTA_PURPLE    , "purple_terracotta"     );
        put(MaterialColor.TERRACOTTA_BLUE      , "blue_terracotta"       );
        put(MaterialColor.TERRACOTTA_BROWN     , "brown_terracotta"      );
        put(MaterialColor.TERRACOTTA_GREEN     , "green_terracotta"      );
        put(MaterialColor.TERRACOTTA_RED       , "red_terracotta"        );
        put(MaterialColor.TERRACOTTA_BLACK     , "black_terracotta"      );
        put(MaterialColor.CRIMSON_NYLIUM        , "crimson_nylium"        );
        put(MaterialColor.CRIMSON_STEM         , "crimson_stem"          );
        put(MaterialColor.CRIMSON_HYPHAE        , "crimson_hyphae"        );
        put(MaterialColor.WARPED_NYLIUM         , "warped_nylium"         );
        put(MaterialColor.WARPED_STEM           , "warped_stem"           );
        put(MaterialColor.WARPED_HYPHAE         , "warped_hyphae"         );
        put(MaterialColor.WARPED_WART_BLOCK           , "warped_wart"           );
        put(MaterialColor.DEEPSLATE           , "deepslate"           );
        put(MaterialColor.RAW_IRON           , "raw_iron"           );
        put(MaterialColor.GLOW_LICHEN           , "glow_lichen"           );

    }};

    public static final Map<Material, String> materialName = new HashMap<Material, String>() {{
        put(Material.AIR            , "air"          );
        put(Material.STRUCTURAL_AIR , "void"         );
        put(Material.PORTAL         , "portal"       );
        put(Material.CLOTH_DECORATION         , "carpet"       );
        put(Material.PLANT          , "plant"        );
        put(Material.WATER_PLANT, "water_plant" );
        put(Material.REPLACEABLE_PLANT, "vegetation"       );
        put(Material.REPLACEABLE_FIREPROOF_PLANT, "nether_shoots"    );
        put(Material.REPLACEABLE_WATER_PLANT, "sea_grass"    );
        put(Material.WATER          , "water"        );
        put(Material.BUBBLE_COLUMN  , "bubble_column");
        put(Material.LAVA           , "lava"         );
        put(Material.TOP_SNOW     , "snow_layer"   );
        put(Material.FIRE           , "fire"         );
        put(Material.DECORATION      , "decoration"   );
        put(Material.WEB         , "cobweb"       );
        put(Material.SCULK         , "sculk"       );
        put(Material.BUILDABLE_GLASS  , "redstone_lamp");
        put(Material.CLAY, "clay"         );
        put(Material.DIRT           , "dirt"         );
        put(Material.GRASS  , "grass"        );
        put(Material.ICE_SOLID      , "packed_ice"   );
        put(Material.SAND      , "sand"         );
        put(Material.SPONGE         , "sponge"       );
        put(Material.SHULKER_SHELL    , "shulker"      );
        put(Material.WOOD           , "wood"         );
        put(Material.NETHER_WOOD    , "nether_wood"  );
        put(Material.BAMBOO_SAPLING , "shoots"       );
        put(Material.BAMBOO         , "bamboo"       );
        put(Material.WOOL           , "wool"         );
        put(Material.EXPLOSIVE            , "tnt"          );
        put(Material.LEAVES         , "leaves"       );
        put(Material.GLASS          , "glass"        );
        put(Material.ICE            , "ice"          );
        put(Material.CACTUS         , "cactus"       );
        put(Material.STONE          , "stone"        );
        put(Material.METAL          , "metal"        );
        put(Material.SNOW     , "snow"         );
        put(Material.HEAVY_METAL , "anvil"        );
        put(Material.BARRIER        , "barrier"      );
        put(Material.PISTON         , "piston"       );
        put(Material.MOSS     , "moss"         );
        put(Material.VEGETABLE          , "gourd"        );
        put(Material.EGG            , "dragon_egg"   );
        put(Material.CAKE           , "cake"         );
        put(Material.AMETHYST       , "amethyst"     );
        put(Material.POWDER_SNOW    , "powder_snow");
    }};

    public static List<BaseComponent> blockInfo(BlockPos pos, ServerLevel world)
    {
        BlockState state = world.getBlockState(pos);
        Material material = state.getMaterial();
        Block block = state.getBlock();
        String metastring = "";
        for (net.minecraft.world.level.block.state.properties.Property<?> iproperty : state.getProperties())
        {
            metastring += ", "+iproperty.getName() + '='+state.getValue(iproperty);
        }
        List<BaseComponent> lst = new ArrayList<>();
        lst.add(Messenger.s(""));
        lst.add(Messenger.s("====================================="));
        lst.add(Messenger.s(String.format("Block info for %s%s (id %d%s):",Registry.BLOCK.getKey(block),metastring, Registry.BLOCK.getId(block), metastring )));
        lst.add(Messenger.s(String.format(" - Material: %s", materialName.get(material))));
        lst.add(Messenger.s(String.format(" - Map colour: %s", mapColourName.get(state.getMapColor(world, pos)))));
        lst.add(Messenger.s(String.format(" - Sound type: %s", soundName.get(block.getSoundType(state)))));
        lst.add(Messenger.s(""));
        lst.add(Messenger.s(String.format(" - Full block: %s", state.isCollisionShapeFullBlock(world, pos)))); //  isFullCube() )));
        lst.add(Messenger.s(String.format(" - Normal cube: %s", state.isRedstoneConductor(world, pos)))); //isNormalCube()))); isSimpleFullBlock
        lst.add(Messenger.s(String.format(" - Is liquid: %s", material.isLiquid())));
        lst.add(Messenger.s(""));
        lst.add(Messenger.s(String.format(" - Light in: %d, above: %d",
                Math.max(world.getBrightness(LightLayer.BLOCK, pos),world.getBrightness(LightLayer.SKY, pos)) ,
                Math.max(world.getBrightness(LightLayer.BLOCK, pos.above()),world.getBrightness(LightLayer.SKY, pos.above())))));
        lst.add(Messenger.s(String.format(" - Brightness in: %.2f, above: %.2f", world.getBrightness(pos), world.getBrightness(pos.above()))));
        lst.add(Messenger.s(String.format(" - Is opaque: %s", material.isSolid() )));
        //lst.add(Messenger.s(String.format(" - Light opacity: %d", state.getOpacity(world,pos))));
        lst.add(Messenger.s(String.format(" - Blocks light: %s", state.getMaterial().isSolidBlocking())));
        //lst.add(Messenger.s(String.format(" - Emitted light: %d", state.getLightValue())));
        //lst.add(Messenger.s(String.format(" - Picks neighbour light value: %s", state.useNeighborBrightness(world, pos))));
        lst.add(Messenger.s(""));
        lst.add(Messenger.s(String.format(" - Causes suffocation: %s", state.isSuffocating(world, pos)))); //canSuffocate
        lst.add(Messenger.s(String.format(" - Blocks movement on land: %s", !state.isPathfindable(world,pos, PathComputationType.LAND))));
        lst.add(Messenger.s(String.format(" - Blocks movement in air: %s", !state.isPathfindable(world,pos, PathComputationType.AIR))));
        lst.add(Messenger.s(String.format(" - Blocks movement in liquids: %s", !state.isPathfindable(world,pos, PathComputationType.WATER))));
        lst.add(Messenger.s(String.format(" - Can burn: %s", material.isFlammable())));
        lst.add(Messenger.s(String.format(" - Requires a tool: %s", !material.isReplaceable()))); //?maybe
        lst.add(Messenger.s(String.format(" - Hardness: %.2f", state.getDestroySpeed(world, pos))));
        lst.add(Messenger.s(String.format(" - Blast resistance: %.2f", block.getExplosionResistance())));
        lst.add(Messenger.s(String.format(" - Ticks randomly: %s", block.isRandomlyTicking(state))));
        lst.add(Messenger.s(""));
        lst.add(Messenger.s(String.format(" - Can provide power: %s", state.isSignalSource())));
        lst.add(Messenger.s(String.format(" - Strong power level: %d", world.getDirectSignalTo(pos))));
        lst.add(Messenger.s(String.format(" - Redstone power level: %d", world.getBestNeighborSignal(pos))));
        lst.add(Messenger.s(""));
        lst.add(wander_chances(pos.above(), world));

        return lst;
    }

    private static BaseComponent wander_chances(BlockPos pos, ServerLevel worldIn)
    {
        PathfinderMob creature = new ZombifiedPiglin(EntityType.ZOMBIFIED_PIGLIN, worldIn);
        creature.finalizeSpawn(worldIn, worldIn.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null, null);
        creature.moveTo(pos, 0.0F, 0.0F);
        RandomStrollGoal wander = new RandomStrollGoal(creature, 0.8D);
        int success = 0;
        for (int i=0; i<1000; i++)
        {

            Vec3 vec = DefaultRandomPos.getPos(creature, 10, 7); // TargetFinder.findTarget(creature, 10, 7);
            if (vec == null)
            {
                continue;
            }
            success++;
        }
        long total_ticks = 0;
        for (int trie=0; trie<1000; trie++)
        {
            int i;
            for (i=1;i<30*20*60; i++) //*60 used to be 5 hours, limited to 30 mins
            {
                if (wander.canUse())
                {
                    break;
                }
            }
            total_ticks += 3*i;
        }
        creature.discard(); // discarded // remove(Entity.RemovalReason.field_26999); // 2nd option - DISCARDED
        long total_time = (total_ticks)/1000/20;
        return Messenger.s(String.format(" - Wander chance above: %.1f%%\n - Average standby above: %s",
                (100.0F*success)/1000,
                ((total_time>5000)?"INFINITY":(total_time +" s"))
        ));
    }
}
