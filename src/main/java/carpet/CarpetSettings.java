package carpet;

import carpet.api.settings.CarpetRule;
import carpet.api.settings.RuleCategory;
import carpet.api.settings.Validators;
import carpet.api.settings.Validator;
import carpet.script.utils.AppStoreManager;
import carpet.settings.Rule;
import carpet.utils.Translations;
import carpet.utils.CommandHelper;
import carpet.utils.Messenger;
import carpet.utils.SpawnChunks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerInterface;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.piston.PistonStructureResolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Locale;
import java.util.Optional;

import static carpet.api.settings.RuleCategory.BUGFIX;
import static carpet.api.settings.RuleCategory.COMMAND;
import static carpet.api.settings.RuleCategory.CREATIVE;
import static carpet.api.settings.RuleCategory.EXPERIMENTAL;
import static carpet.api.settings.RuleCategory.FEATURE;
import static carpet.api.settings.RuleCategory.OPTIMIZATION;
import static carpet.api.settings.RuleCategory.SURVIVAL;
import static carpet.api.settings.RuleCategory.TNT;
import static carpet.api.settings.RuleCategory.DISPENSER;
import static carpet.api.settings.RuleCategory.SCARPET;
import static carpet.api.settings.RuleCategory.CLIENT;

@SuppressWarnings({"CanBeFinal", "removal"}) // removal should be removed after migrating rules to the new system
public class CarpetSettings
{
    public static final String carpetVersion = "1.4.93+v221230";
    public static final String releaseTarget = "1.19.3";
    public static final Logger LOG = LoggerFactory.getLogger("carpet");
    public static final ThreadLocal<Boolean> skipGenerationChecks = ThreadLocal.withInitial(() -> false);
    public static final ThreadLocal<Boolean> impendingFillSkipUpdates = ThreadLocal.withInitial(() -> false);
    public static final int VANILLA_FILL_LIMIT = 32768;
    public static int runPermissionLevel = 2;
    public static boolean doChainStone = false;
    public static boolean chainStoneStickToAll = false;
    public static Block structureBlockIgnoredBlock = Blocks.STRUCTURE_VOID;
    private static class LanguageValidator extends Validator<String> {
        @Override public String validate(CommandSourceStack source, CarpetRule<String> currentRule, String newValue, String string) {
            if (!Translations.isValidLanguage(newValue))
            {
                Messenger.m(source, "r "+newValue+" is not a valid language");
                return null;
            }
            CarpetSettings.language = newValue;
            Translations.updateLanguage();
            return newValue;
        }
    }
    @Rule(
            desc = "Sets the language for Carpet",
            category = FEATURE,
            options = {"en_us", "pt_br", "zh_cn", "zh_tw"},
            strict = true, // the current system doesn't handle fallbacks and other, not defined languages would make unreadable mess. Change later
            validate = LanguageValidator.class
    )
    public static String language = "en_us";

    /*
    These will be turned when events can be added / removed in code
    Then also gotta remember to remove relevant rules

    @Rule(
            desc = "Turns on internal camera path tracing app",
            extra = "Controlled via 'camera' command",
            category = {COMMAND, SCARPET},
            appSource = "camera"
    )
    public static boolean commandCamera = true;

    @Rule(
            desc = "Allows to add extra graphical debug information",
            extra = "Controlled via 'overlay' command",
            category = {COMMAND, SCARPET},
            appSource = "overlay"
    )
    public static boolean commandOverlay = true;

    @Rule(
            desc = "Turns on extra information about mobs above and around them",
            extra = "Controlled via 'ai_tracker' command",
            category = {COMMAND, SCARPET},
            appSource = "ai_tracker"
    )
    public static boolean commandAITracker = true;

    @Rule(
        desc = "Enables /draw commands",
        extra = {
            "... allows for drawing simple shapes or",
            "other shapes which are sorta difficult to do normally"
        },
        appSource = "draw",
        category = {FEATURE, SCARPET, COMMAND}
    )
    public static String commandDraw = "true";

    @Rule(
        desc = "Enables /distance command to measure in game distance between points",
        extra = "Also enables brown carpet placement action if 'carpets' rule is turned on as well",
        appSource = "distance",
        category = {FEATURE, SCARPET, COMMAND}
    )
    public static String commandDistance = "true";
    */

    private static class CarpetPermissionLevel extends Validator<String> {
        @Override public String validate(CommandSourceStack source, CarpetRule<String> currentRule, String newValue, String string) {
            if (source == null || source.hasPermission(4))
                return newValue;
            return null;
        }

        @Override
        public String description()
        {
            return "This setting can only be set by admins with op level 4";
        }
    }
    @Rule(
            desc = "Carpet command permission level. Can only be set via .conf file",
            category = CREATIVE,
            validate = CarpetPermissionLevel.class,
            options = {"ops", "2", "4"}
    )
    public static String carpetCommandPermissionLevel = "ops";



    @Rule(desc = "Gbhs sgnf sadsgras fhskdpri!!!", category = EXPERIMENTAL)
    public static boolean superSecretSetting = false;

    @Rule(
            desc = "Amount of delay ticks to use a nether portal in creative",
            options = {"1", "40", "80", "72000"},
            category = CREATIVE,
            strict = false,
            validate = OneHourMaxDelayLimit.class
    )
    public static int portalCreativeDelay = 1;

    @Rule(
            desc = "Amount of delay ticks to use a nether portal in survival",
            options = {"1", "40", "80", "72000"},
            category = SURVIVAL,
            strict = false,
            validate = OneHourMaxDelayLimit.class
    )
    public static int portalSurvivalDelay = 80;


    private static class OneHourMaxDelayLimit extends Validator<Integer> {
        @Override public Integer validate(CommandSourceStack source, CarpetRule<Integer> currentRule, Integer newValue, String string) {
            return (newValue > 0 && newValue <= 72000) ? newValue : null;
        }
        @Override
        public String description() { return "You must choose a value from 1 to 72000";}
    }

    @Rule(desc = "Dropping entire stacks works also from on the crafting UI result slot", category = {RuleCategory.BUGFIX, SURVIVAL})
    public static boolean ctrlQCraftingFix = false;

    @Rule(desc = "Parrots don't get of your shoulders until you receive proper damage", category = {SURVIVAL, FEATURE})
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

    public static class StackableShulkerBoxValidator extends Validator<String> 
    {
        @Override
        public String validate(CommandSourceStack source, CarpetRule<String> currentRule, String newValue, String string)
        {
            if (newValue.matches("^[0-9]+$")) {
                int value = Integer.parseInt(newValue);
                if (value <= 64 && value >= 2) {
                    shulkerBoxStackSize = value;
                    return newValue;
                }
            }
            if (newValue.equalsIgnoreCase("false")) {
                shulkerBoxStackSize = 1;
                return newValue;
            }
            if (newValue.equalsIgnoreCase("true")) {
                shulkerBoxStackSize = 64;
                return newValue;
            }
            return null;
        }

        @Override
        public String description()
        {
            return "Value must either be true, false, or a number between 2-64";
        }
    }

    @Rule(
            desc = "Empty shulker boxes can stack when thrown on the ground.",
            extra = ".. or when manipulated inside the inventories",
            validate = StackableShulkerBoxValidator.class,
            options = {"false", "true", "16"},
            strict = false,
            category = {SURVIVAL, FEATURE}
    )
    public static String stackableShulkerBoxes = "false";
    public static int shulkerBoxStackSize = 1;

    @Rule( desc = "Explosions won't destroy blocks", category = {CREATIVE, TNT} )
    public static boolean explosionNoBlockDamage = false;

    @Rule( desc = "Experience will drop from all experience barring blocks with any explosion type", category = {SURVIVAL, FEATURE})
    public static boolean xpFromExplosions = false;

    @Rule( desc = "Removes random TNT momentum when primed", category = {CREATIVE, TNT} )
    public static boolean tntPrimerMomentumRemoved = false;

    @Rule( desc = "TNT causes less lag when exploding in the same spot and in liquids", category = TNT)
    public static boolean optimizedTNT = false;

    private static class CheckOptimizedTntEnabledValidator<T> extends Validator<T>
    {
        @Override
        public T validate(CommandSourceStack source, CarpetRule<T> currentRule, T newValue, String string) {
            return optimizedTNT || currentRule.defaultValue().equals(newValue) ? newValue : null;
        }

        @Override
        public String description() {
            return "optimizedTNT must be enabled";
        }
    }

    @Rule( desc = "Sets the tnt random explosion range to a fixed value", category = TNT, options = "-1", strict = false,
            validate = {CheckOptimizedTntEnabledValidator.class, TNTRandomRangeValidator.class}, extra = "Set to -1 for default behavior")
    public static double tntRandomRange = -1;

    private static class TNTRandomRangeValidator extends Validator<Double> {
        @Override
        public Double validate(CommandSourceStack source, CarpetRule<Double> currentRule, Double newValue, String string) {
            return newValue == -1 || newValue >= 0 ? newValue : null;
        }

        @Override
        public String description() {
            return "Cannot be negative, except for -1";
        }
    }

    @Rule( desc = "Sets the horizontal random angle on TNT for debugging of TNT contraptions", category = TNT, options = "-1", strict = false,
            validate = TNTAngleValidator.class, extra = "Set to -1 for default behavior")
    public static double hardcodeTNTangle = -1.0D;

    private static class TNTAngleValidator extends Validator<Double> {
        @Override
        public Double validate(CommandSourceStack source, CarpetRule<Double> currentRule, Double newValue, String string) {
            return (newValue >= 0 && newValue < Math.PI * 2) || newValue == -1 ? newValue : null;
        }

        @Override
        public String description() {
            return "Must be between 0 and 2pi, or -1";
        }
    }

    @Rule( desc = "Merges stationary primed TNT entities", category = TNT )
    public static boolean mergeTNT = false;

    @Rule(
            desc = "Lag optimizations for redstone dust",
            extra = {
                    "by Theosib",
                    ".. also fixes some locational behaviours or vanilla redstone MC-11193",
                    "so behaviour of locational vanilla contraptions is not guaranteed"
            },
            category = {EXPERIMENTAL, OPTIMIZATION}
    )
    public static boolean fastRedstoneDust = false;

    @Rule(desc = "Only husks spawn in desert temples", category = FEATURE)
    public static boolean huskSpawningInTemples = false;

    @Rule( desc = "Shulkers will respawn in end cities", category = FEATURE )
    public static boolean shulkerSpawningInEndCities = false;

    @Rule(
            desc = "Piglins will respawn in bastion remnants",
            extra = "Includes piglins, brutes, and a few hoglins",
            category = FEATURE
    )
    public static boolean piglinsSpawningInBastions = false;

    @Rule( desc = "TNT doesn't update when placed against a power source", category = {CREATIVE, TNT} )
    public static boolean tntDoNotUpdate = false;

    @Rule(
            desc = "Prevents players from rubberbanding when moving too fast",
            extra = {"... or being kicked out for 'flying'",
                    "Puts more trust in clients positioning",
                    "Increases player allowed mining distance to 32 blocks"
            },
            category = {CREATIVE, SURVIVAL}
    )
    public static boolean antiCheatDisabled = false;

    @Rule(desc = "Pistons, droppers and dispensers react if block above them is powered", category = CREATIVE)
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
            category = {COMMAND, CREATIVE, FEATURE}
    )
    public static boolean hopperCounters = false;

    @Rule(
            desc = "Allows Budding Amethyst blocks to be moved",
            extra = {
                    "Allow for them to be moved by pistons",
                    "as well as adds extra drop when mining with silk touch pickaxe"
            },
            category = FEATURE
    )
    public static boolean movableAmethyst = false;

    @Rule( desc = "Guardians turn into Elder Guardian when struck by lightning", category = FEATURE )
    public static boolean renewableSponges = false;

    @Rule( desc = "Pistons can push block entities, like hoppers, chests etc.", category = {EXPERIMENTAL, FEATURE} )
    public static boolean movableBlockEntities = false;


    private static class ChainStoneSetting extends Validator<String> {
        @Override public String validate(CommandSourceStack source, CarpetRule<String> currentRule, String newValue, String string) {
            CarpetSettings.doChainStone = !newValue.toLowerCase(Locale.ROOT).equals("false");
            CarpetSettings.chainStoneStickToAll = newValue.toLowerCase(Locale.ROOT).equals("stick_to_all");

            return newValue;
        }
    }

    @Rule(
            desc = "Chains will stick to each other on the long ends",
            extra = {
                    "and will stick to other blocks that connect to them directly.",
                    "With stick_to_all: it will stick even if not visually connected"
            },
            category = {EXPERIMENTAL, FEATURE},
            options = {"true", "false", "stick_to_all"},
            validate = ChainStoneSetting.class
    )
    public static String chainStone = "false";

    @Rule( desc = "Saplings turn into dead shrubs in hot climates and no water access", category = FEATURE )
    public static boolean desertShrubs = false;

    @Rule( desc = "Silverfish drop a gravel item when breaking out of a block", category = FEATURE )
    public static boolean silverFishDropGravel = false;

    @Rule( desc = "summoning a lightning bolt has all the side effects of natural lightning", category = CREATIVE )
    public static boolean summonNaturalLightning = false;

    @Rule(desc = "Enables /spawn command for spawn tracking", category = COMMAND)
    public static String commandSpawn = "ops";

    @Rule(desc = "Enables /tick command to control game clocks", category = COMMAND)
    public static String commandTick = "ops";

    @Rule(
            desc = "Enables /profile command to monitor game performance",
            extra = "subset of /tick command capabilities",
            category = COMMAND
    )
    public static String commandProfile = "true";

    @Rule(
            desc = "Required permission level for /perf command",
            options = {"2", "4"},
            category = CREATIVE
    )
    public static int perfPermissionLevel = 4;

    @Rule(desc = "Enables /log command to monitor events via chat and overlays", category = COMMAND)
    public static String commandLog = "true";

    @Rule(
            desc = "sets these loggers in their default configurations for all new players",
            extra = "use csv, like 'tps,mobcaps' for multiple loggers, none for nothing",
            category = {CREATIVE, SURVIVAL},
            options = {"none", "tps", "mobcaps,tps"},
            strict = false
    )
    public static String defaultLoggers = "none";

    @Rule(
            desc = "Enables /distance command to measure in game distance between points",
            extra = "Also enables brown carpet placement action if 'carpets' rule is turned on as well",
            category = COMMAND
    )
    public static String commandDistance = "true";

    @Rule(
            desc = "Enables /info command for blocks",
            extra = {
                    "Also enables gray carpet placement action",
                    "if 'carpets' rule is turned on as well"
            },
            category = COMMAND
    )
    public static String commandInfo = "true";

    @Rule(
            desc = "Enables /perimeterinfo command",
            extra = "... that scans the area around the block for potential spawnable spots",
            category = COMMAND
    )
    public static String commandPerimeterInfo = "true";

    @Rule(desc = "Enables /draw commands", extra = {"... allows for drawing simple shapes or","other shapes which are sorta difficult to do normally"}, category = COMMAND)
    public static String commandDraw = "ops";


    @Rule(
            desc = "Enables /script command",
            extra = "An in-game scripting API for Scarpet programming language",
            category = {COMMAND, SCARPET}
    )
    public static String commandScript = "true";

    private static class ModulePermissionLevel extends Validator<String> {
        @Override public String validate(CommandSourceStack source, CarpetRule<String> currentRule, String newValue, String string) {
            int permissionLevel = switch (newValue) {
                    case "false":
                        yield 0;
                    case "true":
                    case "ops":
                        yield 2;
                    case "0":
                    case "1":
                    case "2":
                    case "3":
                    case "4":
                    	yield Integer.parseInt(newValue);
                    default: throw new IllegalArgumentException();
            	};
            if (source != null && !source.hasPermission(permissionLevel))
                return null;
            CarpetSettings.runPermissionLevel = permissionLevel;
            if (source != null)
                CommandHelper.notifyPlayersCommandsChanged(source.getServer());
            return newValue;
        }
        @Override
        public String description() { return "When changing the rule, you must at least have the permission level you are trying to give it";}
    }
    @Rule(
            desc = "Enables restrictions for arbitrary code execution with scarpet",
            extra = {
                    "Users that don't have this permission level",
                    "won't be able to load apps or /script run.",
                    "It is also the permission level apps will",
                    "have when running commands with run()"
            },
            category = {SCARPET},
            options = {"ops", "0", "1", "2", "3", "4"},
            validate = {Validators.CommandLevel.class, ModulePermissionLevel.class}
    )
    public static String commandScriptACE = "ops";

    @Rule(
            desc = "Scarpet script from world files will autoload on server/world start ",
            extra = "if /script is enabled",
            category = SCARPET
    )
    public static boolean scriptsAutoload = true;

    @Rule(
            desc = "Enables scripts debugging messages in system log",
            category = SCARPET
    )
    public static boolean scriptsDebugging = false;

    @Rule(
            desc = "Enables scripts optimization",
            category = SCARPET
    )
    public static boolean scriptsOptimization = true;

    @Rule(
            desc = "Location of the online repository of scarpet apps",
            extra = {
                    "set to 'none' to disable.",
                    "Point to any github repo with scarpet apps",
                    "using <user>/<repo>/contents/<path...>"
            },
            category = SCARPET,
            strict = false,
            validate= AppStoreManager.ScarpetAppStoreValidator.class
    )
    public static String scriptsAppStore = "gnembon/scarpet/contents/programs";


    @Rule(desc = "Enables /player command to control/spawn players", category = COMMAND)
    public static String commandPlayer = "ops";

    @Rule(desc = "Spawn offline players in online mode if online-mode player with specified name does not exist", category = COMMAND)
    public static boolean allowSpawningOfflinePlayers = true;

    @Rule(desc = "Allows to track mobs AI via /track command", category = COMMAND)
    public static String commandTrackAI = "ops";

    @Rule(desc = "Placing carpets may issue carpet commands for non-op players", category = SURVIVAL)
    public static boolean carpets = false;

    @Rule(
            desc = "Glass can be broken faster with pickaxes",
            category = SURVIVAL
    )
    public static boolean missingTools = false;

    @Rule(desc = "fill/clone/setblock and structure blocks cause block updates", category = CREATIVE)
    public static boolean fillUpdates = true;

    @Rule(desc = "placing blocks cause block updates", category = CREATIVE)
    public static boolean interactionUpdates = true;

    @Rule(desc = "Disables breaking of blocks caused by flowing liquids", category = CREATIVE)
    public static boolean liquidDamageDisabled = false;

    @Rule(
            desc = "smooth client animations with low tps settings",
            extra = "works only in SP, and will slow down players",
            category = {CREATIVE, SURVIVAL, CLIENT}
    )
    public static boolean smoothClientAnimations;

    //@Rule(
    //        desc="Fixes mining ghost blocks by trusting clients with block breaking",
    //        extra="Increases player allowed mining distance to 32 blocks",
    //        category = SURVIVAL
    //)
    //public static boolean miningGhostBlockFix = false;

    private static class PushLimitLimits extends Validator<Integer> {
        @Override public Integer validate(CommandSourceStack source, CarpetRule<Integer> currentRule, Integer newValue, String string) {
            return (newValue>0 && newValue <= 1024) ? newValue : null;
        }
        @Override
        public String description() { return "You must choose a value from 1 to 1024";}
    }
    @Rule(
            desc = "Customizable piston push limit",
            options = {"10", "12", "14", "100"},
            category = CREATIVE,
            strict = false,
            validate = PushLimitLimits.class
    )
    public static int pushLimit = PistonStructureResolver.MAX_PUSH_DEPTH;

    @Rule(
            desc = "Customizable powered rail power range",
            options = {"9", "15", "30"},
            category = CREATIVE,
            strict = false,
            validate = PushLimitLimits.class
    )
    public static int railPowerLimit = 9;

    private static class FillLimitLimits extends Validator<Integer> {
        @Override public Integer validate(CommandSourceStack source, CarpetRule<Integer> currentRule, Integer newValue, String string) {
            return (newValue>0 && newValue <= 20000000) ? newValue : null;
        }
        @Override
        public String description() { return "You must choose a value from 1 to 20M";}
    }
    @Rule(
            desc = "Customizable fill/fillbiome/clone volume limit",
            options = {"32768", "250000", "1000000"},
            category = CREATIVE,
            strict = false,
            validate = FillLimitLimits.class
    )
    public static int fillLimit = VANILLA_FILL_LIMIT;


    @Rule(
            desc = "Customizable forceload chunk limit",
            options = {"256"},
            category = CREATIVE,
            strict = false,
            validate = FillLimitLimits.class
    )
    public static int forceloadLimit = 256;

    @Rule(
            desc = "Customizable maximal entity collision limits, 0 for no limits",
            options = {"0", "1", "20"},
            category = OPTIMIZATION,
            strict = false,
            validate = Validators.NonNegativeNumber.class
    )
    public static int maxEntityCollisions = 0;

    @Rule(
            desc = "Customizable server list ping (Multiplayer menu) playerlist sample limit",
            options = {"0", "12", "20", "40"},
            category = CREATIVE,
            strict = false,
            validate = Validators.NonNegativeNumber.class
    )
    public static int pingPlayerListLimit = 12;
    /*

    @Rule(
            desc = "fixes water performance issues",
            category = OPTIMIZATION,
            validate = Validator.WIP.class
    )
    public static boolean waterFlow = true;
    */

    @Rule(
            desc = "Sets a different motd message on client trying to connect to the server",
            extra = "use '_' to use the startup setting from server.properties",
            options = "_",
            strict = false,
            category = CREATIVE
    )
    public static String customMOTD = "_";

    @Rule(
            desc = "Cactus in dispensers rotates blocks.",
            extra = "Rotates block anti-clockwise if possible",
            category = {FEATURE, DISPENSER}
    )
    public static boolean rotatorBlock = false;

    private static class ViewDistanceValidator extends Validator<Integer>
    {
        @Override public Integer validate(CommandSourceStack source, CarpetRule<Integer> currentRule, Integer newValue, String string)
        {
            if (currentRule.value().equals(newValue) || source == null)
            {
                return newValue;
            }
            if (newValue < 0 || newValue > 32)
            {
                Messenger.m(source, "r view distance has to be between 0 and 32");
                return null;
            }
            MinecraftServer server = source.getServer();

            if (server.isDedicatedServer())
            {
                int vd = (newValue >= 2)?newValue:((ServerInterface) server).getProperties().viewDistance;
                if (vd != server.getPlayerList().getViewDistance())
                    server.getPlayerList().setViewDistance(vd);
                return newValue;
            }
            else
            {
                Messenger.m(source, "r view distance can only be changed on a server");
                return 0;
            }
        }
        @Override
        public String description() { return "You must choose a value from 0 (use server settings) to 32";}
    }
    @Rule(
            desc = "Changes the view distance of the server.",
            extra = "Set to 0 to not override the value in server settings.",
            options = {"0", "12", "16", "32"},
            category = CREATIVE,
            strict = false,
            validate = ViewDistanceValidator.class
    )
    public static int viewDistance = 0;

    private static class SimulationDistanceValidator extends Validator<Integer>
    {
        @Override public Integer validate(CommandSourceStack source, CarpetRule<Integer> currentRule, Integer newValue, String string)
        {
            if (currentRule.value().equals(newValue) || source == null)
            {
                return newValue;
            }
            if (newValue < 0 || newValue > 32)
            {
                Messenger.m(source, "r simulation distance has to be between 0 and 32");
                return null;
            }
            MinecraftServer server = source.getServer();

            if (server.isDedicatedServer())
            {
                int vd = (newValue >= 2)?newValue:((DedicatedServer) server).getProperties().simulationDistance;
                if (vd != server.getPlayerList().getSimulationDistance())
                    server.getPlayerList().setSimulationDistance(vd);
                return newValue;
            }
            else
            {
                Messenger.m(source, "r simulation distance can only be changed on a server");
                return 0;
            }
        }
        @Override
        public String description() { return "You must choose a value from 0 (use server settings) to 32";}
    }
    @Rule(
            desc = "Changes the simulation distance of the server.",
            extra = "Set to 0 to not override the value in server settings.",
            options = {"0", "12", "16", "32"},
            category = CREATIVE,
            strict = false,
            validate = SimulationDistanceValidator.class
    )
    public static int simulationDistance = 0;

    public static class ChangeSpawnChunksValidator extends Validator<Integer> {
        public static void changeSpawnSize(int size)
        {
            ServerLevel overworld = CarpetServer.minecraft_server.getLevel(Level.OVERWORLD); // OW
            if (overworld != null) {
                ChunkPos centerChunk = new ChunkPos(new BlockPos(
                        overworld.getLevelData().getXSpawn(),
                        overworld.getLevelData().getYSpawn(),
                        overworld.getLevelData().getZSpawn()
                ));
                SpawnChunks.changeSpawnChunks(overworld.getChunkSource(), centerChunk, size);
            }
        }
        @Override public Integer validate(CommandSourceStack source, CarpetRule<Integer> currentRule, Integer newValue, String string) {
            if (source == null) return newValue;
            if (newValue < 0 || newValue > 32)
            {
                Messenger.m(source, "r spawn chunk size has to be between 0 and 32");
                return null;
            }
            if (currentRule.value().intValue() == newValue.intValue())
            {
                //must been some startup thing
                return newValue;
            }
            if (CarpetServer.minecraft_server == null) return newValue;
            ServerLevel currentOverworld = CarpetServer.minecraft_server.getLevel(Level.OVERWORLD); // OW
            if (currentOverworld != null)
            {
                changeSpawnSize(newValue);
            }
            return newValue;
        }
    }
    @Rule(
            desc = "Changes size of spawn chunks",
            extra = {"Defines new radius", "setting it to 0 - disables spawn chunks"},
            category = CREATIVE,
            strict = false,
            options = {"0", "11"},
            validate = ChangeSpawnChunksValidator.class
    )
    public static int spawnChunksSize = MinecraftServer.START_CHUNK_RADIUS;

    public static class LightBatchValidator extends Validator<Integer> {
        public static void applyLightBatchSizes(int maxBatchSize)
        {
            Iterator<ServerLevel> iterator = CarpetServer.minecraft_server.getAllLevels().iterator();
            
            while (iterator.hasNext()) 
            {
                ServerLevel serverWorld = iterator.next();
                serverWorld.getChunkSource().getLightEngine().setTaskPerBatch(maxBatchSize);
            }
        }
        @Override public Integer validate(CommandSourceStack source, CarpetRule<Integer> currentRule, Integer newValue, String string) {
            if (source == null) return newValue;
            if (newValue < 0)
            {
                Messenger.m(source, "r light batch size has to be at least 0");
                return null;
            }
            if (currentRule.value().intValue() == newValue.intValue())
            {
                //must been some startup thing
                return newValue;
            }
            if (CarpetServer.minecraft_server == null) return newValue;
            
            applyLightBatchSizes(newValue); // Apply new settings
            
            return newValue;
        }
    }
    
    @Rule(
            desc = "Changes maximum light tasks batch size",
            extra = {"Allows for a higher light suppression tolerance", "setting it to 5 - Default limit defined by the game"},
            category = {EXPERIMENTAL, OPTIMIZATION},
            strict = false,
            options = {"5", "50", "100", "200"},
            validate = LightBatchValidator.class
    )
    public static int lightEngineMaxBatchSize = 5;

    public enum RenewableCoralMode {
        FALSE,
        EXPANDED,
        TRUE;
    }
    @Rule(
            desc = "Coral structures will grow with bonemeal from coral plants",
            extra = "Expanded also allows growing from coral fans for sustainable farming outside of warm oceans",
            category = FEATURE
    )
    public static RenewableCoralMode renewableCoral = RenewableCoralMode.FALSE;

    @Rule(
            desc = "Nether basalt generator without soul sand below ",
            extra = "  .. will convert into blackstone instead",
            category = FEATURE
    )
    public static boolean renewableBlackstone = false;

    @Rule(
            desc = "Lava and water generate deepslate and cobbled deepslate instead below Y0",
            category = FEATURE
    )
    public static boolean renewableDeepslate = false;

    @Rule(desc = "fixes block placement rotation issue when player rotates quickly while placing blocks", category = RuleCategory.BUGFIX)
    public static boolean placementRotationFix = false;

    @Rule(
            desc = "Fixes leads breaking/becoming invisible in unloaded chunks",
            extra = "You may still get visibly broken leash links on the client side, but server side the link is still there.",
            category = RuleCategory.BUGFIX
    )// needs checkfix for 1.15
    public static boolean leadFix = false;

    @Rule(desc = "Spawning requires much less CPU and Memory", category = OPTIMIZATION)
    public static boolean lagFreeSpawning = false;
    
    @Rule(
            desc = "Allows structure mobs to spawn in flat worlds",
            category = {EXPERIMENTAL, CREATIVE}
    )
    public static boolean flatWorldStructureSpawning = false;

    @Rule(
            desc = "Increases for testing purposes number of blue skulls shot by the wither",
            category = CREATIVE
    )
    public static boolean moreBlueSkulls = false;

    @Rule(
            desc = "Removes fog from client in the nether and the end",
            extra = "Improves visibility, but looks weird",
            category = CLIENT
    )
    public static boolean fogOff = false;

    @Rule(
            desc = "Creative No Clip",
            extra = {
                    "On servers it needs to be set on both ",
                    "client and server to function properly.",
                    "Has no effect when set on the server only",
                    "Can allow to phase through walls",
                    "if only set on the carpet client side",
                    "but requires some trapdoor magic to",
                    "allow the player to enter blocks"
            },
            category = {CREATIVE, CLIENT}
    )
    public static boolean creativeNoClip = false;
    public static boolean isCreativeFlying(Entity entity)
    {
        // #todo replace after merger to 1.17
        return CarpetSettings.creativeNoClip && entity instanceof Player && (((Player) entity).isCreative()) && ((Player) entity).getAbilities().flying;
    }


    @Rule(
            desc = "Creative flying speed multiplier",
            extra = {
                    "Purely client side setting, meaning that",
                    "having it set on the decicated server has no effect",
                    "but this also means it will work on vanilla servers as well"
            },
            category = {CREATIVE, CLIENT},
            strict = false,
            validate = Validators.NonNegativeNumber.class
    )
    public static double creativeFlySpeed = 1.0;

    @Rule(
            desc = "Creative air drag",
            extra = {
                    "Increased drag will slow down your flight",
                    "So need to adjust speed accordingly",
                    "With 1.0 drag, using speed of 11 seems to matching vanilla speeds.",
                    "Purely client side setting, meaning that",
                    "having it set on the decicated server has no effect",
                    "but this also means it will work on vanilla servers as well"
            },
            category = {CREATIVE, CLIENT},
            strict = false,
            validate = Validators.Probablity.class
    )
    public static double creativeFlyDrag = 0.09;

    @Rule(
            desc = "Removes obnoxious messages from the logs",
            extra = {
                    "Doesn't display 'Maximum sound pool size 247 reached'",
                    "Which is normal with decent farms and contraptions"
            },
            category = {SURVIVAL, CLIENT}
    )
    public static boolean cleanLogs = false;

    public static class StructureBlockLimitValidator extends Validator<Integer> {

        @Override public Integer validate(CommandSourceStack source, CarpetRule<Integer> currentRule, Integer newValue, String string) {
            return (newValue >= StructureBlockEntity.MAX_SIZE_PER_AXIS) ? newValue : null;
        }

        @Override
        public String description() {
            return "You have to choose a value greater or equal to 48";
        }
    }
    @Rule(
            desc = "Customizable structure block limit of each axis",
            extra = {"WARNING: Needs to be permanent for correct loading.",
                    "Setting 'structureBlockIgnored' to air is recommended",
                    "when saving massive structures.",
                    "Required on client of player editing the Structure Block.",
                    "'structureBlockOutlineDistance' may be required for",
                    "correct rendering of long structures."},
            options = {"48", "96", "192", "256"},
            category = CREATIVE,
            validate = StructureBlockLimitValidator.class,
            strict = false
    )
    public static int structureBlockLimit = StructureBlockEntity.MAX_SIZE_PER_AXIS;

    public static class StructureBlockIgnoredValidator extends Validator<String> {

        @Override
        public String validate(CommandSourceStack source, CarpetRule<String> currentRule, String newValue, String string) {
            Optional<Block> ignoredBlock = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.tryParse(newValue));
            if (!ignoredBlock.isPresent()) {
                Messenger.m(source, "r Unknown block '" + newValue + "'.");
                return null;
            }
            structureBlockIgnoredBlock = ignoredBlock.get();
            return newValue;
        }
    }
    @Rule(
            desc = "Changes the block ignored by the Structure Block",
            options = {"minecraft:structure_void", "minecraft:air"},
            category = CREATIVE,
            validate = StructureBlockIgnoredValidator.class,
            strict = false
    )
    public static String structureBlockIgnored = "minecraft:structure_void";

    @Rule(
            desc = "Customizable Structure Block outline render distance",
            extra = "Required on client to work properly",
            options = {"96", "192", "2048"},
            category = {CREATIVE, CLIENT},
            strict = false,
            validate = Validators.NonNegativeNumber.class
    )
    public static int structureBlockOutlineDistance = 96;

    @Rule(
            desc = "Lightning kills the items that drop when lightning kills an entity",
            extra = {"Setting to true will prevent lightning from killing drops", "Fixes [MC-206922](https://bugs.mojang.com/browse/MC-206922)."},
            category = BUGFIX
    )
    public static boolean lightningKillsDropsFix = false;

    @Rule(
            desc = "Placing an activator rail on top of a barrier block will fill the neighbor updater stack when the rail turns off.",
            extra = {"The integer entered is the amount of updates that should be left in the stack", "-1 turns it off"},
            category = CREATIVE,
            options = {"-1","0","10","50"},
            strict = false,
            validate = UpdateSuppressionBlockModes.class
    )
    public static int updateSuppressionBlock = -1;

    private static class UpdateSuppressionBlockModes extends Validator<Integer> {
        @Override
        public Integer validate(CommandSourceStack source, CarpetRule<Integer> currentRule, Integer newValue, String string) {
            return newValue < -1 ? null : newValue;
        }
        @Override
        public String description() {
            return "This value represents the amount of updates required before the logger logs them. Must be -1 or larger";
        }
    }

    @Rule(
            desc = "Creative players load chunks, or they don't! Just like spectators!",
            extra = {"Toggling behaves exactly as if the player is in spectator mode and toggling the gamerule spectatorsGenerateChunks."
            },
            category = {CREATIVE, FEATURE}
    )
    public static boolean creativePlayersLoadChunks = true;

    @Rule(
            desc = "Customizable sculk sensor range",
            options = {"8", "16", "32"},
            category = CREATIVE,
            strict = false,
            validate = PushLimitLimits.class
    )
    public static int sculkSensorRange = 8;

}
