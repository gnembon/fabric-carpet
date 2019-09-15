package carpet.utils;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnType;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.block.Material;
import net.minecraft.block.MaterialColor;
import net.minecraft.block.BlockState;
import net.minecraft.entity.mob.MobEntityWithAi;
import net.minecraft.entity.ai.goal.WanderAroundGoal;
import net.minecraft.entity.ai.PathfindingUtil;
import net.minecraft.entity.mob.ZombiePigmanEntity;
import net.minecraft.block.BlockPlacementEnvironment;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.text.BaseText;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockInfo
{
    public static Map<BlockSoundGroup, String> soundName = new HashMap<BlockSoundGroup, String>() {{
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
    }};

    public static Map<MaterialColor, String> mapColourName = new HashMap<MaterialColor, String>() {{
        put(MaterialColor.AIR       , "air"       );
        put(MaterialColor.GRASS     , "grass"     );
        put(MaterialColor.SAND      , "sand"      );
        put(MaterialColor.WEB       , "wool"      );
        put(MaterialColor.LAVA      , "tnt"       );
        put(MaterialColor.ICE       , "ice"       );
        put(MaterialColor.IRON      , "iron"      );
        put(MaterialColor.FOLIAGE   , "foliage"   );
        put(MaterialColor.WHITE     , "snow"      );
        put(MaterialColor.CLAY      , "clay"      );
        put(MaterialColor.DIRT      , "dirt"      );
        put(MaterialColor.STONE     , "stone"     );
        put(MaterialColor.WATER     , "water"     );
        put(MaterialColor.WOOD      , "wood"      );
        put(MaterialColor.QUARTZ    , "quartz"    );
        put(MaterialColor.ORANGE    , "adobe"     );
        put(MaterialColor.MAGENTA   , "magenta"   );
        put(MaterialColor.LIGHT_BLUE, "light_blue");
        put(MaterialColor.YELLOW    , "yellow"    );
        put(MaterialColor.LIME      , "lime"      );
        put(MaterialColor.PINK      , "pink"      );
        put(MaterialColor.GRAY      , "gray"      );
        put(MaterialColor.LIGHT_GRAY, "light_gray");
        put(MaterialColor.CYAN      , "cyan"      );
        put(MaterialColor.PURPLE    , "purple"    );
        put(MaterialColor.BLUE      , "blue"      );
        put(MaterialColor.BROWN     , "brown"     );
        put(MaterialColor.GREEN     , "green"     );
        put(MaterialColor.RED       , "red"       );
        put(MaterialColor.BLACK     , "black"     );
        put(MaterialColor.GOLD      , "gold"      );
        put(MaterialColor.DIAMOND   , "diamond"   );
        put(MaterialColor.LAPIS     , "lapis"     );
        put(MaterialColor.EMERALD   , "emerald"   );
        put(MaterialColor.SPRUCE  , "obsidian"  );
        put(MaterialColor.NETHER,   "netherrack"); //TODO fix these
        put(MaterialColor.WHITE_TERRACOTTA     , "white_terracotta"      );
        put(MaterialColor.ORANGE_TERRACOTTA    , "orange_terracotta"     );
        put(MaterialColor.MAGENTA_TERRACOTTA   , "magenta_terracotta"    );
        put(MaterialColor.LIGHT_BLUE_TERRACOTTA, "light_blue_terracotta" );
        put(MaterialColor.YELLOW_TERRACOTTA    , "yellow_terracotta"     );
        put(MaterialColor.LIME_TERRACOTTA      , "lime_terracotta"       );
        put(MaterialColor.PINK_TERRACOTTA      , "pink_terracotta"       );
        put(MaterialColor.GRAY_TERRACOTTA      , "gray_terracotta"       );
        put(MaterialColor.LIGHT_GRAY_TERRACOTTA, "light_gray_terracotta" );
        put(MaterialColor.CYAN_TERRACOTTA      , "cyan_terracotta"       );
        put(MaterialColor.PURPLE_TERRACOTTA    , "purple_terracotta"     );
        put(MaterialColor.BLUE_TERRACOTTA      , "blue_terracotta"       );
        put(MaterialColor.BROWN_TERRACOTTA     , "brown_terracotta"      );
        put(MaterialColor.GREEN_TERRACOTTA     , "green_terracotta"      );
        put(MaterialColor.RED_TERRACOTTA       , "red_terracotta"        );
        put(MaterialColor.BLACK_TERRACOTTA     , "black_terracotta"      );
    }};

    public static Map<Material, String> materialName = new HashMap<Material, String>() {{
        put(Material.AIR            , "air"          );
        put(Material.STRUCTURE_VOID , "void"         );
        put(Material.PORTAL         , "portal"       );
        put(Material.CARPET         , "carpet"       );
        put(Material.PLANT          , "plant"        );
        put(Material.UNDERWATER_PLANT    , "water_plant"  );
        put(Material.REPLACEABLE_PLANT           , "vine"         );
        put(Material.SEAGRASS       , "sea_grass"    );
        put(Material.WATER          , "water"        );
        put(Material.BUBBLE_COLUMN  , "bubble_column");
        put(Material.LAVA           , "lava"         );
        put(Material.SNOW           , "snow_layer"   );
        put(Material.FIRE           , "fire"         );
        put(Material.PART           , "redstone_bits");
        put(Material.COBWEB            , "cobweb"       );
        put(Material.REDSTONE_LAMP  , "redstone_lamp");
        put(Material.CLAY           , "clay"         );
        put(Material.EARTH          , "dirt"         );
        put(Material.ORGANIC        , "grass"        );
        put(Material.PACKED_ICE     , "packed_ice"   );
        put(Material.SAND           , "sand"         );
        put(Material.SPONGE         , "sponge"       );
        put(Material.SHULKER_BOX           , "wood"         );
        put(Material.WOOD           , "wool"         ); // fix these
        put(Material.BAMBOO_SAPLING     , "shoots"          );
        put(Material.BAMBOO         , "bamboo"       );
        put(Material.TNT            , "tnt"          );
        put(Material.LEAVES         , "leaves"       );
        put(Material.GLASS          , "glass"        );
        put(Material.ICE            , "ice"          );
        put(Material.CACTUS         , "cactus"       );
        put(Material.STONE          , "stone"        );
        put(Material.METAL          , "iron"         );
        put(Material.SNOW_BLOCK     , "snow"         );
        put(Material.ANVIL          , "anvil"        );
        put(Material.BARRIER        , "barrier"      );
        put(Material.PISTON         , "piston"       );
        put(Material.UNUSED_PLANT   , "coral"        );
        put(Material.PUMPKIN        , "gourd"        );
        put(Material.EGG            , "dragon_egg"   );
        put(Material.CAKE           , "cake"         );
    }};

    public static List<BaseText> blockInfo(BlockPos pos, World world)
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
        lst.add(Messenger.s(String.format(" - Map colour: %s", mapColourName.get(state.getTopMaterialColor(world, pos)))));
        lst.add(Messenger.s(String.format(" - Sound type: %s", soundName.get(block.getSoundGroup(state)))));
        lst.add(Messenger.s(""));
        //lst.add(Messenger.s(String.format(" - Full block: %s", block.isShapeFullCube()))); //  isFullCube() )));
        lst.add(Messenger.s(String.format(" - Normal cube: %s", state.isSimpleFullBlock(world, pos)))); //isNormalCube())));
        lst.add(Messenger.s(String.format(" - Is liquid: %s", material.isLiquid())));
        lst.add(Messenger.s(""));
        lst.add(Messenger.s(String.format(" - Light in: %d, above: %d", world.getLightLevel(pos), world.getLightLevel(pos.up()))));
        lst.add(Messenger.s(String.format(" - Brightness in: %.2f, above: %.2f", world.getBrightness(pos), world.getBrightness(pos.up()))));
        lst.add(Messenger.s(String.format(" - Is opaque: %s", material.isSolid() )));
        //lst.add(Messenger.s(String.format(" - Light opacity: %d", state.getOpacity(world,pos))));
        lst.add(Messenger.s(String.format(" - Blocks light: %s", state.getMaterial().blocksLight())));
        //lst.add(Messenger.s(String.format(" - Emitted light: %d", state.getLightValue())));
        //lst.add(Messenger.s(String.format(" - Picks neighbour light value: %s", state.useNeighborBrightness(world, pos))));
        lst.add(Messenger.s(""));
        lst.add(Messenger.s(String.format(" - Causes suffocation: %s", state.canSuffocate(world, pos))));
        lst.add(Messenger.s(String.format(" - Blocks movement on land: %s", !state.canPlaceAtSide(world,pos, BlockPlacementEnvironment.LAND))));
        lst.add(Messenger.s(String.format(" - Blocks movement in air: %s", !state.canPlaceAtSide(world,pos, BlockPlacementEnvironment.AIR))));
        lst.add(Messenger.s(String.format(" - Blocks movement in liquids: %s", !state.canPlaceAtSide(world,pos, BlockPlacementEnvironment.WATER))));
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

    private static BaseText wander_chances(BlockPos pos, World worldIn)
    {
        MobEntityWithAi creature = new ZombiePigmanEntity(EntityType.ZOMBIE_PIGMAN, worldIn);
        creature.initialize(worldIn, worldIn.getLocalDifficulty(pos), SpawnType.NATURAL, null, null);
        creature.setPositionAndAngles(pos.getX()+0.5F, pos.getY(), pos.getZ()+0.5F, 0.0F, 0.0F);
        WanderAroundGoal wander = new WanderAroundGoal(creature, 0.8D);
        int success = 0;
        for (int i=0; i<1000; i++)
        {

            Vec3d vec = PathfindingUtil.findTarget(creature, 10, 7);
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
        creature.remove();
        long total_time = (total_ticks)/1000/20;
        return Messenger.s(String.format(" - Wander chance above: %.1f%%\n - Average standby above: %s",
                (100.0F*success)/1000,
                ((total_time>5000)?"INFINITY":(Long.toString(total_time)+" s"))
        ));
    }
}
