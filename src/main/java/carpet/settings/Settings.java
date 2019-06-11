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
