package carpet.utils;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.block.MapColor;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.goal.WanderAroundGoal;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.text.BaseText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockInfo
{
    public static final Map<BlockSoundGroup, String> soundName = new HashMap<BlockSoundGroup, String>() {{
        put(BlockSoundGroup.WOOD,   "wood"  );
        put(BlockSoundGroup.GRAVEL, "gravel");
        put(BlockSoundGroup.GRASS,  "grass" );
        put(BlockSoundGroup.LILY_PAD, "lily_pad");
        put(BlockSoundGroup.STONE,  "stone" );
        put(BlockSoundGroup.METAL,  "metal" );
        put(BlockSoundGroup.GLASS , "glass" );
        put(BlockSoundGroup.WOOL  , "wool"  );
        put(BlockSoundGroup.SAND  , "sand"  );
        put(BlockSoundGroup.SNOW  , "snow"  );
        put(BlockSoundGroup.POWDER_SNOW  , "powder_snow"  );
        put(BlockSoundGroup.LADDER, "ladder");
        put(BlockSoundGroup.ANVIL , "anvil" );
        put(BlockSoundGroup.SLIME  , "slime"  );
        put(BlockSoundGroup.HONEY  , "honey"  );
        put(BlockSoundGroup.WET_GRASS , "sea_grass" );
        put(BlockSoundGroup.CORAL , "coral" );
        put(BlockSoundGroup.BAMBOO , "bamboo" );
        put(BlockSoundGroup.BAMBOO_SAPLING , "shoots" );
        put(BlockSoundGroup.SCAFFOLDING , "scaffolding" );
        put(BlockSoundGroup.SWEET_BERRY_BUSH , "berry" );
        put(BlockSoundGroup.CROP , "crop" );
        put(BlockSoundGroup.STEM , "stem" );
        put(BlockSoundGroup.VINE , "vine" );
        put(BlockSoundGroup.NETHER_WART , "wart" );
        put(BlockSoundGroup.LANTERN , "lantern" );
        put(BlockSoundGroup.NETHER_STEM, "fungi_stem");
        put(BlockSoundGroup.NYLIUM, "nylium");
        put(BlockSoundGroup.FUNGUS, "fungus");
        put(BlockSoundGroup.ROOTS, "roots");
        put(BlockSoundGroup.SHROOMLIGHT, "shroomlight");
        put(BlockSoundGroup.WEEPING_VINES, "weeping_vine");
        put(BlockSoundGroup.WEEPING_VINES_LOW_PITCH, "twisting_vine");
        put(BlockSoundGroup.SOUL_SAND, "soul_sand");
        put(BlockSoundGroup.SOUL_SOIL, "soul_soil");
        put(BlockSoundGroup.BASALT, "basalt");
        put(BlockSoundGroup.WART_BLOCK, "wart");
        put(BlockSoundGroup.NETHERRACK, "netherrack");
        put(BlockSoundGroup.NETHER_BRICKS, "nether_bricks");
        put(BlockSoundGroup.NETHER_SPROUTS, "nether_sprouts");
        put(BlockSoundGroup.NETHER_ORE, "nether_ore");
        put(BlockSoundGroup.BONE, "bone");
        put(BlockSoundGroup.NETHERITE, "netherite");
        put(BlockSoundGroup.ANCIENT_DEBRIS, "ancient_debris");
        put(BlockSoundGroup.LODESTONE, "lodestone");
        put(BlockSoundGroup.CHAIN, "chain");
        put(BlockSoundGroup.NETHER_GOLD_ORE, "nether_gold_ore");
        put(BlockSoundGroup.GILDED_BLACKSTONE, "gilded_blackstone");
        put(BlockSoundGroup.CANDLE, "candle");
        put(BlockSoundGroup.AMETHYST_BLOCK, "amethyst");
        put(BlockSoundGroup.AMETHYST_CLUSTER, "amethyst_cluster");
        put(BlockSoundGroup.SMALL_AMETHYST_BUD, "small_amethyst_bud");
        put(BlockSoundGroup.MEDIUM_AMETHYST_BUD, "medium_amethyst_bud");
        put(BlockSoundGroup.LARGE_AMETHYST_BUD, "large_amethyst_bud");

        put(BlockSoundGroup.TUFF, "tuff");
        put(BlockSoundGroup.CALCITE, "calcite");
        put(BlockSoundGroup.DRIPSTONE_BLOCK, "dripstone");
        put(BlockSoundGroup.POINTED_DRIPSTONE, "pointed_dripstone");
        put(BlockSoundGroup.COPPER, "copper");
        put(BlockSoundGroup.CAVE_VINES, "cave_vine");
        put(BlockSoundGroup.SPORE_BLOSSOM, "spore_blossom");
        put(BlockSoundGroup.AZALEA, "azalea");
        put(BlockSoundGroup.FLOWERING_AZALEA, "flowering_azalea");
        put(BlockSoundGroup.MOSS_CARPET, "moss_carpet");
        put(BlockSoundGroup.MOSS_BLOCK, "moss");
        put(BlockSoundGroup.BIG_DRIPLEAF, "big_dripleaf");
        put(BlockSoundGroup.SMALL_DRIPLEAF, "small_dripleaf");
        put(BlockSoundGroup.ROOTED_DIRT, "rooted_dirt");
        put(BlockSoundGroup.HANGING_ROOTS, "hanging_roots");
        put(BlockSoundGroup.AZALEA_LEAVES, "azalea_leaves");
        put(BlockSoundGroup.SCULK_SENSOR, "sculk_sensor");
        put(BlockSoundGroup.GLOW_LICHEN, "glow_lichen");
        put(BlockSoundGroup.DEEPSLATE, "deepslate");
        put(BlockSoundGroup.DEEPSLATE_BRICKS, "deepslate_bricks");
        put(BlockSoundGroup.DEEPSLATE_TILES, "deepslate_tiles");
        put(BlockSoundGroup.POLISHED_DEEPSLATE, "polished_deepslate");
    }};

    public static final Map<MapColor, String> mapColourName = new HashMap<MapColor, String>() {{
        put(MapColor.CLEAR     , "air"       );
        put(MapColor.PALE_GREEN     , "grass"     );
        put(MapColor.PALE_YELLOW       , "sand"      );
        put(MapColor.WHITE_GRAY        , "wool"      );
        put(MapColor.BRIGHT_RED       , "tnt"       );
        put(MapColor.PALE_PURPLE        , "ice"       );
        put(MapColor.IRON_GRAY      , "iron"      );
        put(MapColor.DARK_GREEN    , "foliage"   );
        put(MapColor.WHITE     , "snow"      );
        put(MapColor.LIGHT_BLUE_GRAY       , "clay"      );
        put(MapColor.DIRT_BROWN       , "dirt"      );
        put(MapColor.STONE_GRAY      , "stone"     );
        put(MapColor.WATER_BLUE      , "water"     );
        put(MapColor.OAK_TAN       , "wood"      );
        put(MapColor.OFF_WHITE     , "quartz"    );
        put(MapColor.ORANGE    , "adobe"     );
        put(MapColor.MAGENTA   , "magenta"   );
        put(MapColor.LIGHT_BLUE, "light_blue");
        put(MapColor.YELLOW    , "yellow"    );
        put(MapColor.LIME      , "lime"      );
        put(MapColor.PINK      , "pink"      );
        put(MapColor.GRAY      , "gray"      );
        put(MapColor.LIGHT_GRAY, "light_gray");
        put(MapColor.CYAN      , "cyan"      );
        put(MapColor.PURPLE    , "purple"    );
        put(MapColor.BLUE      , "blue"      );
        put(MapColor.BROWN     , "brown"     );
        put(MapColor.GREEN     , "green"     );
        put(MapColor.RED       , "red"       );
        put(MapColor.BLACK     , "black"     );
        put(MapColor.GOLD      , "gold"      );
        put(MapColor.DIAMOND_BLUE    , "diamond"   );
        put(MapColor.LAPIS_BLUE      , "lapis"     );
        put(MapColor.EMERALD_GREEN    , "emerald"   );
        put(MapColor.SPRUCE_BROWN     , "obsidian"  );
        put(MapColor.DARK_RED     , "netherrack"); //TODO fix these
        put(MapColor.TERRACOTTA_WHITE      , "white_terracotta"      );
        put(MapColor.TERRACOTTA_ORANGE    , "orange_terracotta"     );
        put(MapColor.TERRACOTTA_MAGENTA   , "magenta_terracotta"    );
        put(MapColor.TERRACOTTA_LIGHT_BLUE, "light_blue_terracotta" );
        put(MapColor.TERRACOTTA_YELLOW    , "yellow_terracotta"     );
        put(MapColor.TERRACOTTA_LIME      , "lime_terracotta"       );
        put(MapColor.TERRACOTTA_PINK      , "pink_terracotta"       );
        put(MapColor.TERRACOTTA_GRAY      , "gray_terracotta"       );
        put(MapColor.TERRACOTTA_LIGHT_GRAY, "light_gray_terracotta" );
        put(MapColor.TERRACOTTA_CYAN      , "cyan_terracotta"       );
        put(MapColor.TERRACOTTA_PURPLE    , "purple_terracotta"     );
        put(MapColor.TERRACOTTA_BLUE      , "blue_terracotta"       );
        put(MapColor.TERRACOTTA_BROWN     , "brown_terracotta"      );
        put(MapColor.TERRACOTTA_GREEN     , "green_terracotta"      );
        put(MapColor.TERRACOTTA_RED       , "red_terracotta"        );
        put(MapColor.TERRACOTTA_BLACK     , "black_terracotta"      );
        put(MapColor.DULL_RED        , "crimson_nylium"        );
        put(MapColor.DULL_PINK         , "crimson_stem"          );
        put(MapColor.DARK_CRIMSON        , "crimson_hyphae"        );
        put(MapColor.TEAL         , "warped_nylium"         );
        put(MapColor.DARK_AQUA           , "warped_stem"           );
        put(MapColor.DARK_DULL_PINK         , "warped_hyphae"         );
        put(MapColor.BRIGHT_TEAL           , "warped_wart"           );
        put(MapColor.DEEPSLATE_GRAY           , "deepslate"           );
        put(MapColor.RAW_IRON_PINK           , "raw_iron"           );
        put(MapColor.LICHEN_GREEN           , "glow_lichen"           );

    }};

    public static final Map<Material, String> materialName = new HashMap<Material, String>() {{
        put(Material.AIR            , "air"          );
        put(Material.STRUCTURE_VOID , "void"         );
        put(Material.PORTAL         , "portal"       );
        put(Material.CARPET         , "carpet"       );
        put(Material.PLANT          , "plant"        );
        put(Material.UNDERWATER_PLANT, "water_plant" );
        put(Material.REPLACEABLE_PLANT, "vegetation"       );
        put(Material.NETHER_SHOOTS, "nether_shoots"    );
        put(Material.REPLACEABLE_UNDERWATER_PLANT, "sea_grass"    );
        put(Material.WATER          , "water"        );
        put(Material.BUBBLE_COLUMN  , "bubble_column");
        put(Material.LAVA           , "lava"         );
        put(Material.SNOW_LAYER     , "snow_layer"   );
        put(Material.FIRE           , "fire"         );
        put(Material.DECORATION      , "decoration"   );
        put(Material.COBWEB         , "cobweb"       );
        put(Material.SCULK         , "sculk"       );
        put(Material.REDSTONE_LAMP  , "redstone_lamp");
        put(Material.ORGANIC_PRODUCT, "clay"         );
        put(Material.SOIL           , "dirt"         );
        put(Material.SOLID_ORGANIC  , "grass"        );
        put(Material.DENSE_ICE      , "packed_ice"   );
        put(Material.AGGREGATE      , "sand"         );
        put(Material.SPONGE         , "sponge"       );
        put(Material.SHULKER_BOX    , "shulker"      );
        put(Material.WOOD           , "wood"         );
        put(Material.NETHER_WOOD    , "nether_wood"  );
        put(Material.BAMBOO_SAPLING , "shoots"       );
        put(Material.BAMBOO         , "bamboo"       );
        put(Material.WOOL           , "wool"         );
        put(Material.TNT            , "tnt"          );
        put(Material.LEAVES         , "leaves"       );
        put(Material.GLASS          , "glass"        );
        put(Material.ICE            , "ice"          );
        put(Material.CACTUS         , "cactus"       );
        put(Material.STONE          , "stone"        );
        put(Material.METAL          , "metal"        );
        put(Material.SNOW_BLOCK     , "snow"         );
        put(Material.REPAIR_STATION , "anvil"        );
        put(Material.BARRIER        , "barrier"      );
        put(Material.PISTON         , "piston"       );
        put(Material.MOSS_BLOCK     , "moss"         );
        put(Material.GOURD          , "gourd"        );
        put(Material.EGG            , "dragon_egg"   );
        put(Material.CAKE           , "cake"         );
        put(Material.AMETHYST       , "amethyst"     );
        put(Material.POWDER_SNOW    , "powder_snow");
    }};

    public static List<BaseText> blockInfo(BlockPos pos, ServerWorld world)
    {
        BlockState state = world.getBlockState(pos);
        Material material = state.getMaterial();
        Block block = state.getBlock();
        String metastring = "";
        for (net.minecraft.state.property.Property<?> iproperty : state.getProperties())
        {
            metastring += ", "+iproperty.getName() + '='+state.get(iproperty);
        }
        List<BaseText> lst = new ArrayList<>();
        lst.add(Messenger.s(""));
        lst.add(Messenger.s("====================================="));
        lst.add(Messenger.s(String.format("Block info for %s%s (id %d%s):",Registry.BLOCK.getId(block),metastring, Registry.BLOCK.getRawId(block), metastring )));
        lst.add(Messenger.s(String.format(" - Material: %s", materialName.get(material))));
        lst.add(Messenger.s(String.format(" - Map colour: %s", mapColourName.get(state.getMapColor(world, pos)))));
        lst.add(Messenger.s(String.format(" - Sound type: %s", soundName.get(block.getSoundGroup(state)))));
        lst.add(Messenger.s(""));
        lst.add(Messenger.s(String.format(" - Full block: %s", state.isFullCube(world, pos)))); //  isFullCube() )));
        lst.add(Messenger.s(String.format(" - Normal cube: %s", state.isSolidBlock(world, pos)))); //isNormalCube()))); isSimpleFullBlock
        lst.add(Messenger.s(String.format(" - Is liquid: %s", material.isLiquid())));
        lst.add(Messenger.s(""));
        lst.add(Messenger.s(String.format(" - Light in: %d, above: %d",
                Math.max(world.getLightLevel(LightType.BLOCK, pos),world.getLightLevel(LightType.SKY, pos)) ,
                Math.max(world.getLightLevel(LightType.BLOCK, pos.up()),world.getLightLevel(LightType.SKY, pos.up())))));
        lst.add(Messenger.s(String.format(" - Brightness in: %.2f, above: %.2f", world.getBrightness(pos), world.getBrightness(pos.up()))));
        lst.add(Messenger.s(String.format(" - Is opaque: %s", material.isSolid() )));
        //lst.add(Messenger.s(String.format(" - Light opacity: %d", state.getOpacity(world,pos))));
        lst.add(Messenger.s(String.format(" - Blocks light: %s", state.getMaterial().blocksLight())));
        //lst.add(Messenger.s(String.format(" - Emitted light: %d", state.getLightValue())));
        //lst.add(Messenger.s(String.format(" - Picks neighbour light value: %s", state.useNeighborBrightness(world, pos))));
        lst.add(Messenger.s(""));
        lst.add(Messenger.s(String.format(" - Causes suffocation: %s", state.shouldSuffocate(world, pos)))); //canSuffocate
        lst.add(Messenger.s(String.format(" - Blocks movement on land: %s", !state.canPathfindThrough(world,pos, NavigationType.LAND))));
        lst.add(Messenger.s(String.format(" - Blocks movement in air: %s", !state.canPathfindThrough(world,pos, NavigationType.AIR))));
        lst.add(Messenger.s(String.format(" - Blocks movement in liquids: %s", !state.canPathfindThrough(world,pos, NavigationType.WATER))));
        lst.add(Messenger.s(String.format(" - Can burn: %s", material.isBurnable())));
        lst.add(Messenger.s(String.format(" - Requires a tool: %s", !material.isReplaceable()))); //?maybe
        lst.add(Messenger.s(String.format(" - Hardness: %.2f", state.getHardness(world, pos))));
        lst.add(Messenger.s(String.format(" - Blast resistance: %.2f", block.getBlastResistance())));
        lst.add(Messenger.s(String.format(" - Ticks randomly: %s", block.hasRandomTicks(state))));
        lst.add(Messenger.s(""));
        lst.add(Messenger.s(String.format(" - Can provide power: %s", state.emitsRedstonePower())));
        lst.add(Messenger.s(String.format(" - Strong power level: %d", world.getReceivedStrongRedstonePower(pos))));
        lst.add(Messenger.s(String.format(" - Redstone power level: %d", world.getReceivedRedstonePower(pos))));
        lst.add(Messenger.s(""));
        lst.add(wander_chances(pos.up(), world));

        return lst;
    }

    private static BaseText wander_chances(BlockPos pos, ServerWorld worldIn)
    {
        PathAwareEntity creature = new ZombifiedPiglinEntity(EntityType.ZOMBIFIED_PIGLIN, worldIn);
        creature.initialize(worldIn, worldIn.getLocalDifficulty(pos), SpawnReason.NATURAL, null, null);
        creature.refreshPositionAndAngles(pos, 0.0F, 0.0F);
        WanderAroundGoal wander = new WanderAroundGoal(creature, 0.8D);
        int success = 0;
        for (int i=0; i<1000; i++)
        {

            Vec3d vec = NoPenaltyTargeting.find(creature, 10, 7); // TargetFinder.findTarget(creature, 10, 7);
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
                if (wander.canStart())
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
