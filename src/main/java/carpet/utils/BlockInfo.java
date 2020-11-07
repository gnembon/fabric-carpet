package carpet.utils;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.block.MapColor;
import net.minecraft.class_5532; // TargetFinder
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
//import net.minecraft.entity.ai.TargetFinder;
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
        put(BlockSoundGroup.STONE,  "stone" );
        put(BlockSoundGroup.METAL,  "metal" );
        put(BlockSoundGroup.GLASS , "glass" );
        put(BlockSoundGroup.WOOL  , "wool"  );
        put(BlockSoundGroup.SAND  , "sand"  );
        put(BlockSoundGroup.SNOW  , "snow"  );
        put(BlockSoundGroup.LADDER, "ladder");
        put(BlockSoundGroup.ANVIL , "anvil" );
        put(BlockSoundGroup.SLIME , "slime" );
        put(BlockSoundGroup.WET_GRASS , "sea_grass" );
        put(BlockSoundGroup.CORAL , "coral" );
        put(BlockSoundGroup.BAMBOO , "bamboo" );
        put(BlockSoundGroup.BAMBOO_SAPLING , "shoots" );
        put(BlockSoundGroup.SCAFFOLDING , "scaffolding" );
        put(BlockSoundGroup.SWEET_BERRY_BUSH , "berry" );
        put(BlockSoundGroup.CROP , "crop" );
        put(BlockSoundGroup.STEM , "stem" );
        put(BlockSoundGroup.NETHER_WART , "wart" );
        put(BlockSoundGroup.LANTERN , "lantern" );
        put(BlockSoundGroup.NETHER_STEM, "fungi_stem");
        put(BlockSoundGroup.NYLIUM, "nylium");
        put(BlockSoundGroup.FUNGUS, "fungus");
        put(BlockSoundGroup.ROOTS, "roots");
        put(BlockSoundGroup.SHROOMLIGHT, "shroomlight");
        put(BlockSoundGroup.WEEPING_VINES, "weeping_vines");
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
        put(BlockSoundGroup.field_27196, "candle");
        put(BlockSoundGroup.field_27197, "amethyst");
        put(BlockSoundGroup.field_27198, "amethyst_cluster");
        put(BlockSoundGroup.field_27199, "small_amethyst_bud");
        put(BlockSoundGroup.field_27200, "large_amethyst_bud");
        put(BlockSoundGroup.field_27201, "medium_amethyst_bud");
        put(BlockSoundGroup.field_27202, "tuff");
        put(BlockSoundGroup.field_27203, "calcite");
        put(BlockSoundGroup.field_27204, "copper");
    }};

    public static final Map<MapColor, String> mapColourName = new HashMap<MapColor, String>() {{
        put(MapColor.CLEAR     , "air"       );
        put(MapColor.GRASS     , "grass"     );
        put(MapColor.SAND      , "sand"      );
        put(MapColor.WEB       , "wool"      );
        put(MapColor.LAVA      , "tnt"       );
        put(MapColor.ICE       , "ice"       );
        put(MapColor.IRON      , "iron"      );
        put(MapColor.FOLIAGE   , "foliage"   );
        put(MapColor.WHITE     , "snow"      );
        put(MapColor.CLAY      , "clay"      );
        put(MapColor.DIRT      , "dirt"      );
        put(MapColor.STONE     , "stone"     );
        put(MapColor.WATER     , "water"     );
        put(MapColor.WOOD      , "wood"      );
        put(MapColor.QUARTZ    , "quartz"    );
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
        put(MapColor.DIAMOND   , "diamond"   );
        put(MapColor.LAPIS     , "lapis"     );
        put(MapColor.EMERALD   , "emerald"   );
        put(MapColor.SPRUCE    , "obsidian"  );
        put(MapColor.NETHER    , "netherrack"); //TODO fix these
        put(MapColor.WHITE_TERRACOTTA     , "white_terracotta"      );
        put(MapColor.ORANGE_TERRACOTTA    , "orange_terracotta"     );
        put(MapColor.MAGENTA_TERRACOTTA   , "magenta_terracotta"    );
        put(MapColor.LIGHT_BLUE_TERRACOTTA, "light_blue_terracotta" );
        put(MapColor.YELLOW_TERRACOTTA    , "yellow_terracotta"     );
        put(MapColor.LIME_TERRACOTTA      , "lime_terracotta"       );
        put(MapColor.PINK_TERRACOTTA      , "pink_terracotta"       );
        put(MapColor.GRAY_TERRACOTTA      , "gray_terracotta"       );
        put(MapColor.LIGHT_GRAY_TERRACOTTA, "light_gray_terracotta" );
        put(MapColor.CYAN_TERRACOTTA      , "cyan_terracotta"       );
        put(MapColor.PURPLE_TERRACOTTA    , "purple_terracotta"     );
        put(MapColor.BLUE_TERRACOTTA      , "blue_terracotta"       );
        put(MapColor.BROWN_TERRACOTTA     , "brown_terracotta"      );
        put(MapColor.GREEN_TERRACOTTA     , "green_terracotta"      );
        put(MapColor.RED_TERRACOTTA       , "red_terracotta"        );
        put(MapColor.BLACK_TERRACOTTA     , "black_terracotta"      );
        put(MapColor.CRIMSON_NYLIUM       , "crimson_nylium"        );
        put(MapColor.CRIMSON_STEM         , "crimson_stem"          );
        put(MapColor.CRIMSON_HYPHAE       , "crimson_hyphae"        );
        put(MapColor.WARPED_NYLIUM        , "warped_nylium"         );
        put(MapColor.WARPED_STEM          , "warped_stem"           );
        put(MapColor.WARPED_HYPHAE        , "warped_hyphae"         );
        put(MapColor.WARPED_WART          , "warped_wart"           );
    }};

    public static final Map<Material, String> materialName = new HashMap<Material, String>() {{
        put(Material.AIR            , "air"          );
        put(Material.STRUCTURE_VOID , "void"         );
        put(Material.PORTAL         , "portal"       );
        put(Material.CARPET         , "carpet"       );
        put(Material.PLANT          , "plant"        );
        put(Material.UNDERWATER_PLANT, "water_plant" );
        put(Material.REPLACEABLE_PLANT, "vegetation"       );
        put(Material.REPLACEABLE_UNDERWATER_PLANT, "sea_grass"    );
        put(Material.WATER          , "water"        );
        put(Material.BUBBLE_COLUMN  , "bubble_column");
        put(Material.LAVA           , "lava"         );
        put(Material.SNOW_LAYER     , "snow_layer"   );
        put(Material.FIRE           , "fire"         );
        put(Material.SUPPORTED      , "decoration"   );
        put(Material.COBWEB         , "cobweb"       );
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
        put(Material.UNUSED_PLANT   , "coral"        );
        put(Material.GOURD          , "gourd"        );
        put(Material.EGG            , "dragon_egg"   );
        put(Material.CAKE           , "cake"         );
        put(Material.AMETHYST       , "amethyst"     );
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

            Vec3d vec = class_5532.method_31510(creature, 10, 7); // TargetFinder.findTarget(creature, 10, 7);
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
