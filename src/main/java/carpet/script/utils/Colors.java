package carpet.script.utils;

import net.minecraft.world.level.material.MapColor;

import java.util.Map;

import static java.util.Map.entry;

public class Colors
{
    public static final Map<MapColor, String> mapColourName = Map.ofEntries(
            entry(MapColor.NONE     , "air"       ),
            entry(MapColor.GRASS     , "grass"     ),
            entry(MapColor.SAND       , "sand"      ),
            entry(MapColor.WOOL        , "wool"      ),
            entry(MapColor.FIRE       , "tnt"       ),
            entry(MapColor.ICE        , "ice"       ),
            entry(MapColor.METAL      , "iron"      ),
            entry(MapColor.PLANT    , "foliage"   ),
            entry(MapColor.SNOW     , "snow"      ),
            entry(MapColor.CLAY       , "clay"      ),
            entry(MapColor.DIRT       , "dirt"      ),
            entry(MapColor.STONE      , "stone"     ),
            entry(MapColor.WATER      , "water"     ),
            entry(MapColor.WOOD       , "wood"      ),
            entry(MapColor.QUARTZ     , "quartz"    ),
            entry(MapColor.COLOR_ORANGE    , "adobe"     ),
            entry(MapColor.COLOR_MAGENTA   , "magenta"   ),
            entry(MapColor.COLOR_LIGHT_BLUE, "light_blue"),
            entry(MapColor.COLOR_YELLOW    , "yellow"    ),
            entry(MapColor.COLOR_LIGHT_GREEN      , "lime"      ),
            entry(MapColor.COLOR_PINK      , "pink"      ),
            entry(MapColor.COLOR_GRAY      , "gray"      ),
            entry(MapColor.COLOR_LIGHT_GRAY, "light_gray"),
            entry(MapColor.COLOR_CYAN      , "cyan"      ),
            entry(MapColor.COLOR_PURPLE    , "purple"    ),
            entry(MapColor.COLOR_BLUE      , "blue"      ),
            entry(MapColor.COLOR_BROWN     , "brown"     ),
            entry(MapColor.COLOR_GREEN     , "green"     ),
            entry(MapColor.COLOR_RED       , "red"       ),
            entry(MapColor.COLOR_BLACK     , "black"     ),
            entry(MapColor.GOLD      , "gold"      ),
            entry(MapColor.DIAMOND    , "diamond"   ),
            entry(MapColor.LAPIS      , "lapis"     ),
            entry(MapColor.EMERALD    , "emerald"   ),
            entry(MapColor.PODZOL     , "obsidian"  ),
            entry(MapColor.NETHER     , "netherrack"), //TODO fix these
            entry(MapColor.TERRACOTTA_WHITE      , "white_terracotta"      ),
            entry(MapColor.TERRACOTTA_ORANGE    , "orange_terracotta"     ),
            entry(MapColor.TERRACOTTA_MAGENTA   , "magenta_terracotta"    ),
            entry(MapColor.TERRACOTTA_LIGHT_BLUE, "light_blue_terracotta" ),
            entry(MapColor.TERRACOTTA_YELLOW    , "yellow_terracotta"     ),
            entry(MapColor.TERRACOTTA_LIGHT_GREEN      , "lime_terracotta"       ),
            entry(MapColor.TERRACOTTA_PINK      , "pink_terracotta"       ),
            entry(MapColor.TERRACOTTA_GRAY      , "gray_terracotta"       ),
            entry(MapColor.TERRACOTTA_LIGHT_GRAY, "light_gray_terracotta" ),
            entry(MapColor.TERRACOTTA_CYAN      , "cyan_terracotta"       ),
            entry(MapColor.TERRACOTTA_PURPLE    , "purple_terracotta"     ),
            entry(MapColor.TERRACOTTA_BLUE      , "blue_terracotta"       ),
            entry(MapColor.TERRACOTTA_BROWN     , "brown_terracotta"      ),
            entry(MapColor.TERRACOTTA_GREEN     , "green_terracotta"      ),
            entry(MapColor.TERRACOTTA_RED       , "red_terracotta"        ),
            entry(MapColor.TERRACOTTA_BLACK     , "black_terracotta"      ),
            entry(MapColor.CRIMSON_NYLIUM        , "crimson_nylium"        ),
            entry(MapColor.CRIMSON_STEM         , "crimson_stem"          ),
            entry(MapColor.CRIMSON_HYPHAE        , "crimson_hyphae"        ),
            entry(MapColor.WARPED_NYLIUM         , "warped_nylium"         ),
            entry(MapColor.WARPED_STEM           , "warped_stem"           ),
            entry(MapColor.WARPED_HYPHAE         , "warped_hyphae"         ),
            entry(MapColor.WARPED_WART_BLOCK           , "warped_wart"           ),
            entry(MapColor.DEEPSLATE           , "deepslate"           ),
            entry(MapColor.RAW_IRON           , "raw_iron"           ),
            entry(MapColor.GLOW_LICHEN           , "glow_lichen"           )
    );
}
