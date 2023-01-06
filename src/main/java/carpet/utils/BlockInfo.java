package carpet.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
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

import static java.util.Map.entry;

public class BlockInfo
{
    public static final Map<SoundType, String> soundName = Map.ofEntries(
        entry(SoundType.WOOD,   "wood"  ),
        entry(SoundType.GRAVEL, "gravel"),
        entry(SoundType.GRASS,  "grass" ),
        entry(SoundType.LILY_PAD, "lily_pad"),
        entry(SoundType.STONE,  "stone" ),
        entry(SoundType.METAL,  "metal" ),
        entry(SoundType.GLASS , "glass" ),
        entry(SoundType.WOOL  , "wool"  ),
        entry(SoundType.SAND  , "sand"  ),
        entry(SoundType.SNOW  , "snow"  ),
        entry(SoundType.POWDER_SNOW  , "powder_snow"  ),
        entry(SoundType.LADDER, "ladder"),
        entry(SoundType.ANVIL , "anvil" ),
        entry(SoundType.SLIME_BLOCK  , "slime"  ),
        entry(SoundType.HONEY_BLOCK  , "honey"  ),
        entry(SoundType.WET_GRASS , "sea_grass" ),
        entry(SoundType.CORAL_BLOCK , "coral" ),
        entry(SoundType.BAMBOO , "bamboo" ),
        entry(SoundType.BAMBOO_SAPLING , "shoots" ),
        entry(SoundType.SCAFFOLDING , "scaffolding" ),
        entry(SoundType.SWEET_BERRY_BUSH , "berry" ),
        entry(SoundType.CROP , "crop" ),
        entry(SoundType.HARD_CROP , "stem" ),
        entry(SoundType.VINE , "vine" ),
        entry(SoundType.NETHER_WART , "wart" ),
        entry(SoundType.LANTERN , "lantern" ),
        entry(SoundType.STEM, "fungi_stem"),
        entry(SoundType.NYLIUM, "nylium"),
        entry(SoundType.FUNGUS, "fungus"),
        entry(SoundType.ROOTS, "roots"),
        entry(SoundType.SHROOMLIGHT, "shroomlight"),
        entry(SoundType.WEEPING_VINES, "weeping_vine"),
        entry(SoundType.TWISTING_VINES, "twisting_vine"),
        entry(SoundType.SOUL_SAND, "soul_sand"),
        entry(SoundType.SOUL_SOIL, "soul_soil"),
        entry(SoundType.BASALT, "basalt"),
        entry(SoundType.WART_BLOCK, "wart"),
        entry(SoundType.NETHERRACK, "netherrack"),
        entry(SoundType.NETHER_BRICKS, "nether_bricks"),
        entry(SoundType.NETHER_SPROUTS, "nether_sprouts"),
        entry(SoundType.NETHER_ORE, "nether_ore"),
        entry(SoundType.BONE_BLOCK, "bone"),
        entry(SoundType.NETHERITE_BLOCK, "netherite"),
        entry(SoundType.ANCIENT_DEBRIS, "ancient_debris"),
        entry(SoundType.LODESTONE, "lodestone"),
        entry(SoundType.CHAIN, "chain"),
        entry(SoundType.NETHER_GOLD_ORE, "nether_gold_ore"),
        entry(SoundType.GILDED_BLACKSTONE, "gilded_blackstone"),
        entry(SoundType.CANDLE, "candle"),
        entry(SoundType.AMETHYST, "amethyst"),
        entry(SoundType.AMETHYST_CLUSTER, "amethyst_cluster"),
        entry(SoundType.SMALL_AMETHYST_BUD, "small_amethyst_bud"),
        entry(SoundType.MEDIUM_AMETHYST_BUD, "medium_amethyst_bud"),
        entry(SoundType.LARGE_AMETHYST_BUD, "large_amethyst_bud"),

        entry(SoundType.TUFF, "tuff"),
        entry(SoundType.CALCITE, "calcite"),
        entry(SoundType.DRIPSTONE_BLOCK, "dripstone"),
        entry(SoundType.POINTED_DRIPSTONE, "pointed_dripstone"),
        entry(SoundType.COPPER, "copper"),
        entry(SoundType.CAVE_VINES, "cave_vine"),
        entry(SoundType.SPORE_BLOSSOM, "spore_blossom"),
        entry(SoundType.AZALEA, "azalea"),
        entry(SoundType.FLOWERING_AZALEA, "flowering_azalea"),
        entry(SoundType.MOSS_CARPET, "moss_carpet"),
        entry(SoundType.MOSS, "moss"),
        entry(SoundType.BIG_DRIPLEAF, "big_dripleaf"),
        entry(SoundType.SMALL_DRIPLEAF, "small_dripleaf"),
        entry(SoundType.ROOTED_DIRT, "rooted_dirt"),
        entry(SoundType.HANGING_ROOTS, "hanging_roots"),
        entry(SoundType.AZALEA_LEAVES, "azalea_leaves"),
        entry(SoundType.SCULK_SENSOR, "sculk_sensor"),
        entry(SoundType.GLOW_LICHEN, "glow_lichen"),
        entry(SoundType.DEEPSLATE, "deepslate"),
        entry(SoundType.DEEPSLATE_BRICKS, "deepslate_bricks"),
        entry(SoundType.DEEPSLATE_TILES, "deepslate_tiles"),
        entry(SoundType.POLISHED_DEEPSLATE, "polished_deepslate")
    );

    public static final Map<MaterialColor, String> mapColourName = Map.ofEntries(
        entry(MaterialColor.NONE     , "air"       ),
        entry(MaterialColor.GRASS     , "grass"     ),
        entry(MaterialColor.SAND       , "sand"      ),
        entry(MaterialColor.WOOL        , "wool"      ),
        entry(MaterialColor.FIRE       , "tnt"       ),
        entry(MaterialColor.ICE        , "ice"       ),
        entry(MaterialColor.METAL      , "iron"      ),
        entry(MaterialColor.PLANT    , "foliage"   ),
        entry(MaterialColor.SNOW     , "snow"      ),
        entry(MaterialColor.CLAY       , "clay"      ),
        entry(MaterialColor.DIRT       , "dirt"      ),
        entry(MaterialColor.STONE      , "stone"     ),
        entry(MaterialColor.WATER      , "water"     ),
        entry(MaterialColor.WOOD       , "wood"      ),
        entry(MaterialColor.QUARTZ     , "quartz"    ),
        entry(MaterialColor.COLOR_ORANGE    , "adobe"     ),
        entry(MaterialColor.COLOR_MAGENTA   , "magenta"   ),
        entry(MaterialColor.COLOR_LIGHT_BLUE, "light_blue"),
        entry(MaterialColor.COLOR_YELLOW    , "yellow"    ),
        entry(MaterialColor.COLOR_LIGHT_GREEN      , "lime"      ),
        entry(MaterialColor.COLOR_PINK      , "pink"      ),
        entry(MaterialColor.COLOR_GRAY      , "gray"      ),
        entry(MaterialColor.COLOR_LIGHT_GRAY, "light_gray"),
        entry(MaterialColor.COLOR_CYAN      , "cyan"      ),
        entry(MaterialColor.COLOR_PURPLE    , "purple"    ),
        entry(MaterialColor.COLOR_BLUE      , "blue"      ),
        entry(MaterialColor.COLOR_BROWN     , "brown"     ),
        entry(MaterialColor.COLOR_GREEN     , "green"     ),
        entry(MaterialColor.COLOR_RED       , "red"       ),
        entry(MaterialColor.COLOR_BLACK     , "black"     ),
        entry(MaterialColor.GOLD      , "gold"      ),
        entry(MaterialColor.DIAMOND    , "diamond"   ),
        entry(MaterialColor.LAPIS      , "lapis"     ),
        entry(MaterialColor.EMERALD    , "emerald"   ),
        entry(MaterialColor.PODZOL     , "obsidian"  ),
        entry(MaterialColor.NETHER     , "netherrack"), //TODO fix these
        entry(MaterialColor.TERRACOTTA_WHITE      , "white_terracotta"      ),
        entry(MaterialColor.TERRACOTTA_ORANGE    , "orange_terracotta"     ),
        entry(MaterialColor.TERRACOTTA_MAGENTA   , "magenta_terracotta"    ),
        entry(MaterialColor.TERRACOTTA_LIGHT_BLUE, "light_blue_terracotta" ),
        entry(MaterialColor.TERRACOTTA_YELLOW    , "yellow_terracotta"     ),
        entry(MaterialColor.TERRACOTTA_LIGHT_GREEN      , "lime_terracotta"       ),
        entry(MaterialColor.TERRACOTTA_PINK      , "pink_terracotta"       ),
        entry(MaterialColor.TERRACOTTA_GRAY      , "gray_terracotta"       ),
        entry(MaterialColor.TERRACOTTA_LIGHT_GRAY, "light_gray_terracotta" ),
        entry(MaterialColor.TERRACOTTA_CYAN      , "cyan_terracotta"       ),
        entry(MaterialColor.TERRACOTTA_PURPLE    , "purple_terracotta"     ),
        entry(MaterialColor.TERRACOTTA_BLUE      , "blue_terracotta"       ),
        entry(MaterialColor.TERRACOTTA_BROWN     , "brown_terracotta"      ),
        entry(MaterialColor.TERRACOTTA_GREEN     , "green_terracotta"      ),
        entry(MaterialColor.TERRACOTTA_RED       , "red_terracotta"        ),
        entry(MaterialColor.TERRACOTTA_BLACK     , "black_terracotta"      ),
        entry(MaterialColor.CRIMSON_NYLIUM        , "crimson_nylium"        ),
        entry(MaterialColor.CRIMSON_STEM         , "crimson_stem"          ),
        entry(MaterialColor.CRIMSON_HYPHAE        , "crimson_hyphae"        ),
        entry(MaterialColor.WARPED_NYLIUM         , "warped_nylium"         ),
        entry(MaterialColor.WARPED_STEM           , "warped_stem"           ),
        entry(MaterialColor.WARPED_HYPHAE         , "warped_hyphae"         ),
        entry(MaterialColor.WARPED_WART_BLOCK           , "warped_wart"           ),
        entry(MaterialColor.DEEPSLATE           , "deepslate"           ),
        entry(MaterialColor.RAW_IRON           , "raw_iron"           ),
        entry(MaterialColor.GLOW_LICHEN           , "glow_lichen"           )
    );

    public static final Map<Material, String> materialName = Map.ofEntries(
        entry(Material.AIR            , "air"          ),
        entry(Material.STRUCTURAL_AIR , "void"         ),
        entry(Material.PORTAL         , "portal"       ),
        entry(Material.CLOTH_DECORATION         , "carpet"       ),
        entry(Material.PLANT          , "plant"        ),
        entry(Material.WATER_PLANT, "water_plant" ),
        entry(Material.REPLACEABLE_PLANT, "vegetation"       ),
        entry(Material.REPLACEABLE_FIREPROOF_PLANT, "nether_shoots"    ),
        entry(Material.REPLACEABLE_WATER_PLANT, "sea_grass"    ),
        entry(Material.WATER          , "water"        ),
        entry(Material.BUBBLE_COLUMN  , "bubble_column"),
        entry(Material.LAVA           , "lava"         ),
        entry(Material.TOP_SNOW     , "snow_layer"   ),
        entry(Material.FIRE           , "fire"         ),
        entry(Material.DECORATION      , "decoration"   ),
        entry(Material.WEB         , "cobweb"       ),
        entry(Material.SCULK         , "sculk"       ),
        entry(Material.BUILDABLE_GLASS  , "redstone_lamp"),
        entry(Material.CLAY, "clay"         ),
        entry(Material.DIRT           , "dirt"         ),
        entry(Material.GRASS  , "grass"        ),
        entry(Material.ICE_SOLID      , "packed_ice"   ),
        entry(Material.SAND      , "sand"         ),
        entry(Material.SPONGE         , "sponge"       ),
        entry(Material.SHULKER_SHELL    , "shulker"      ),
        entry(Material.WOOD           , "wood"         ),
        entry(Material.NETHER_WOOD    , "nether_wood"  ),
        entry(Material.BAMBOO_SAPLING , "shoots"       ),
        entry(Material.BAMBOO         , "bamboo"       ),
        entry(Material.WOOL           , "wool"         ),
        entry(Material.EXPLOSIVE            , "tnt"          ),
        entry(Material.LEAVES         , "leaves"       ),
        entry(Material.GLASS          , "glass"        ),
        entry(Material.ICE            , "ice"          ),
        entry(Material.CACTUS         , "cactus"       ),
        entry(Material.STONE          , "stone"        ),
        entry(Material.METAL          , "metal"        ),
        entry(Material.SNOW     , "snow"         ),
        entry(Material.HEAVY_METAL , "anvil"        ),
        entry(Material.BARRIER        , "barrier"      ),
        entry(Material.PISTON         , "piston"       ),
        entry(Material.MOSS     , "moss"         ),
        entry(Material.VEGETABLE          , "gourd"        ),
        entry(Material.EGG            , "dragon_egg"   ),
        entry(Material.CAKE           , "cake"         ),
        entry(Material.AMETHYST       , "amethyst"     ),
        entry(Material.POWDER_SNOW    , "powder_snow")
    );

    public static List<Component> blockInfo(BlockPos pos, ServerLevel world)
    {
        BlockState state = world.getBlockState(pos);
        Material material = state.getMaterial();
        Block block = state.getBlock();
        String metastring = "";
        for (net.minecraft.world.level.block.state.properties.Property<?> iproperty : state.getProperties())
        {
            metastring += ", "+iproperty.getName() + '='+state.getValue(iproperty);
        }
        List<Component> lst = new ArrayList<>();
        lst.add(Messenger.s(""));
        lst.add(Messenger.s("====================================="));
        lst.add(Messenger.s(String.format("Block info for %s%s (id %d%s):", BuiltInRegistries.BLOCK.getKey(block),metastring, BuiltInRegistries.BLOCK.getId(block), metastring )));
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
        lst.add(Messenger.s(String.format(" - Brightness in: %.2f, above: %.2f", world.getLightLevelDependentMagicValue(pos), world.getLightLevelDependentMagicValue(pos.above()))));
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

    private static Component wander_chances(BlockPos pos, ServerLevel worldIn)
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
