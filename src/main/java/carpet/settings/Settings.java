package carpet.settings;

import net.minecraft.server.command.ServerCommandSource;

import static carpet.settings.RuleCategory.BUGFIX;
import static carpet.settings.RuleCategory.COMMANDS;
import static carpet.settings.RuleCategory.CREATIVE;
import static carpet.settings.RuleCategory.EXPERIMENTAL;
import static carpet.settings.RuleCategory.FEATURE;
import static carpet.settings.RuleCategory.OPTIMIZATIONS;
import static carpet.settings.RuleCategory.SURVIVAL;

public class Settings
{
    @Rule(
            desc = "Fixes server crashing supposedly on falling behind 60s in ONE tick, yeah bs.",
            extra = "Fixed 1.12 watchdog crash in 1.13 pre-releases, reintroduced with 1.13, GG.",
            category = BUGFIX,
            validate = Validator.WIP.class
    )
    public static boolean watchdogCrashFix = false;

    @Rule(
            desc = "Nether portals correctly place entities going through",
            extra = "Entities shouldn't suffocate in obsidian",
            category = BUGFIX,
            validate = Validator.WIP.class
    )
    public static boolean portalSuffocationFix = false;

    @Rule(
            desc = "Gbhs sgnf sadsgras fhskdpri!",
            category = EXPERIMENTAL,
    )
    public static boolean superSecretSetting = false;

    @Rule(
            desc = "Portals won't let a creative player go through instantly",
            extra = "Holding obsidian in either hand won't let you through at all",
            category = CREATIVE
    )
    public static boolean portalCreativeDelay = false;


    @Rule(
            desc = "Dropping entire stacks works also from on the crafting UI result slot",
            category = {BUGFIX, SURVIVAL}
    )
    public static boolean ctrlQCraftingFix = false;


    @Rule(
            desc = "Parrots don't get of your shoulders until you receive damage",
            extra = "",
            category = {SURVIVAL, FEATURE},
            validate = Validator.WIP.class
    )
    public static boolean persistentParrots = false;


    /*@Rule(
            desc = "Mobs growing up won't glitch into walls or go through fences",
            category = BUGFIX,
            validate = Validator.WIP.class
    )
    public static boolean growingUpWallJump = false;


    @Rule(
            desc = "Won't let mobs glitch into blocks when reloaded.",
            extra = "Can cause slight differences in mobs behaviour",
            category = {BUGFIX, EXPERIMENTAL},
            validate = Validator.WIP.class
    )
    public static boolean reloadSuffocationFix = false;
    */



    @Rule( desc = "Players absorb XP instantly, without delay", category = CREATIVE )
    public static boolean xpNoCooldown = false;


    @Rule( desc = "XP orbs combine with other into bigger orbs", category = FEATURE )
    public static boolean combineXPOrbs = false;


    @Rule(
            desc = "Empty shulker boxes can stack to 64 when dropped on the ground",
            extra = "To move them around between inventories, use shift click to move entire stacks",
            category = {SURVIVAL, FEATURE}
    )
    public static boolean stackableShulkerBoxes = false;


    @Rule( desc = "Explosions won't destroy blocks", category = CREATIVE )
    public static boolean explosionNoBlockDamage = false;

    @Rule( desc = "Removes random TNT momentum when primed", category = CREATIVE )
    public static boolean tntPrimerMomentumRemoved = false;

    @Rule(
            desc = "Lag optimizations for redstone dust",
            extra = "by Theosib",
            category = {EXPERIMENTAL, OPTIMIZATIONS},
            validate = Validator.WIP.class
    )
    public static boolean fastRedstoneDust = false;

    @Rule(
            desc = "Only husks spawn in desert temples",
            category = FEATURE,
            validate = Validator.WIP.class
    )
    public static boolean huskSpawningInTemples = false;

    @Rule( desc = "Shulkers will respawn in end cities", category = FEATURE )
    public static boolean shulkerSpawningInEndCities = false;

    @Rule(
            desc = "Entities pushed or moved into unloaded chunks no longer disappear",
            category = {EXPERIMENTAL, BUGFIX},
            validate = Validator.WIP.class
    )
    public static boolean unloadedEntityFix = false;

    @Rule( desc = "TNT doesn't update when placed against a power source", category = CREATIVE )
    public static boolean TNTDoNotUpdate = false;

    @Rule(
            desc = "Prevents players from rubberbanding when moving too fast",
            extra = "Puts more trust in clients positioning",
            category = {CREATIVE, SURVIVAL},
            validate = Validator.WIP.class
    )
    public static boolean antiCheatSpeed = false;

    @Rule(
            desc = "Pistons, droppers and dispensers react if block above them is powered",
            category = CREATIVE,
            validate = Validator.WIP.class
    )
    public static boolean quasiConnectivity = true;

    @Rule(
            desc = "Players can flip and rotate blocks when holding cactus",
            extra = {
                    "Doesn't cause block updates when rotated/flipped",
                    "Applies to pistons, observers, droppers, repeaters, stairs, glazed terracotta etc..."
            },
            category = {CREATIVE, SURVIVAL, FEATURE}
    )
    public static boolean flippinCactus = false;

    @Rule(
            desc = "hoppers pointing to wool will count items passing through them",
            extra = {
                    "Enables /counter command, and actions while placing red and green carpets on wool blocks",
                    "Use /counter <color?> reset to reset the counter, and /counter <color?> to query",
                    "In survival, place green carpet on same color wool to query, red to reset the counters",
                    "Counters are global and shared between players, 16 channels available",
                    "Items counted are destroyed, count up to one stack per tick per hopper"
            },
            category = {COMMANDS, CREATIVE, FEATURE}
    )
    public static boolean hopperCounters = false;

    @Rule( desc = "Guardians turn into Elder Guardian when struck by lightning", category = FEATURE )
    public static boolean renewableSponges = false;

    @Rule( desc = "Pistons can push tile entities, like hoppers, chests etc.", category = {EXPERIMENTAL, FEATURE} )
    public static boolean movableTileEntities = false;

    @Rule( desc = "Saplings turn into dead shrubs in hot climates and no water access", category = FEATURE )
    public static boolean desertShrubs = false;

    @Rule( desc = "Silverfish drop a gravel item when breaking out of a block", category = FEATURE )
    public static boolean silverFishDropGravel = false;

    @Rule( desc = "summoning a lightning bolt has all the side effects of natural lightning", category = CREATIVE )
    public static boolean summonNaturalLightning = false;

    @Rule(
            desc = "",
            category = CREATIVE,
            validate = Validator.WIP.class
    )
    public static boolean aaa = false;

    @Rule(
            desc = "",
            category = CREATIVE,
            validate = Validator.WIP.class
    )
    public static boolean aaa = false;

    @Rule(
            desc = "",
            category = CREATIVE,
            validate = Validator.WIP.class
    )
    public static boolean aaa = false;

    @Rule(
            desc = "",
            category = CREATIVE,
            validate = Validator.WIP.class
    )
    public static boolean aaa = false;

    @Rule(
            desc = "",
            category = CREATIVE,
            validate = Validator.WIP.class
    )
    public static boolean aaa = false;

    @Rule(
            desc = "",
            category = CREATIVE,
            validate = Validator.WIP.class
    )
    public static boolean aaa = false;

















    @Rule(
        desc = "Enables /c and /s commands to quickly switch between camera and survival modes",
        extra = "/c and /s commands are available to all players regardless of their permission levels",
        category = COMMANDS
    )
    public static boolean commandCameramode = true;

    @Rule(desc = "fill/clone/setblock and structure blocks cause block updates", category = CREATIVE)
    public static boolean fillUpdates = true;


    private static class CheckFillLimitLimits extends Validator<Integer>
    {
        @Override
        Integer validate(ServerCommandSource source, ParsedRule<Integer> currentRule, Integer newValue, String typedString)
        {
            return newValue < 20000000 ? newValue : null;
        }
    }
    @Rule(
        desc = "Customizable fill/clone volume limit",
        options = {"32768", "250000", "1000000"},
        validate = {Validator.POSITIVE_NUMBER.class, CheckFillLimitLimits.class},
        category = {CREATIVE, "skyblock"}
    )
    public static int fillLimit = 32768;

    @Rule(
        desc = "Hoppers pointing to wool will count items passing through them",
        extra = {
            "Enables /counter command, and actions while placing red and green carpets on wool blocks",
            "Use /counter <color?> reset to reset the counter, and /counter <color?> to query",
            "In survival, place green carpet on same color wool to query, red to reset the counters",
            "Counters are global and shared between players, 16 channels available",
            "Items counted are destroyed, count up to one stack per tick per hopper"
        },
        category = COMMANDS
    )
    public static boolean hopperCounters = false;

    @Rule(desc = "Explosions won't destroy blocks", category = CREATIVE)
    public static boolean explosionNoBlockDamage = false;

    @Rule(
        desc = "Silverfish drop a gravel item when breaking out of a block",
        category = {FEATURE, EXPERIMENTAL}
    )
    public static boolean silverFishDropGravel = false;

    @Rule(desc = "Shulkers will respawn in end cities", category = {FEATURE, EXPERIMENTAL})
    public static boolean shulkerSpawningInEndCities = false;

    @Rule(
        desc = "Portals won't let a creative player go through instantly",
        extra = "Holding obsidian in either hand won't let you through at all",
        category = CREATIVE
    )
    public static boolean portalCreativeDelay = false;

    @Rule(desc = "Pistons can push block entities, like hoppers, chests etc.", category = {FEATURE, EXPERIMENTAL})
    public static boolean movableTileEntities = false;
    
    @Rule(
        desc = "Empty shulker boxes can stack to 64 when dropped on the ground",
        extra = "To move them around between inventories, use shift click to move entire stacks",
        category = SURVIVAL
    )
    public static boolean stackableShulkerBoxes = false;

    @Rule(desc = "Optimizes spawning", category = {OPTIMIZATIONS, EXPERIMENTAL})
    public static boolean lagFreeSpawning = false;

}
