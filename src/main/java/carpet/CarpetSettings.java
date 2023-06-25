package carpet;

import carpet.api.settings.CarpetRule;
import carpet.api.settings.Rule;
import carpet.api.settings.RuleCategory;
import carpet.api.settings.Validators;
import carpet.api.settings.Validator;
import carpet.script.utils.AppStoreManager;
import carpet.script.external.Carpet;
import carpet.utils.Translations;
import carpet.utils.CommandHelper;
import carpet.utils.Messenger;
import carpet.utils.SpawnChunks;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerInterface;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.border.BorderStatus;
import net.minecraft.world.level.border.WorldBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

@SuppressWarnings("CanBeFinal")
public class CarpetSettings
{
    public static final String carpetVersion = FabricLoader.getInstance().getModContainer("carpet").orElseThrow().getMetadata().getVersion().toString();
    public static final String releaseTarget = "1.19.4";
    public static final Logger LOG = LoggerFactory.getLogger("carpet");
    public static final ThreadLocal<Boolean> skipGenerationChecks = ThreadLocal.withInitial(() -> false);
    public static final ThreadLocal<Boolean> impendingFillSkipUpdates = ThreadLocal.withInitial(() -> false);
    public static int runPermissionLevel = 2;
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
            categories = FEATURE,
            options = {"en_us", "pt_br", "zh_cn", "zh_tw"},
            strict = true, // the current system doesn't handle fallbacks and other, not defined languages would make unreadable mess. Change later
            validators = LanguageValidator.class
    )
    public static String language = "en_us";

    /*
    These will be turned when events can be added / removed in code
    Then also gotta remember to remove relevant rules

    @Rule(
            desc = "Turns on internal camera path tracing app",
            extra = "Controlled via 'camera' command",
            categories = {COMMAND, SCARPET},
            appSource = "camera"
    )
    public static boolean commandCamera = true;

    @Rule(
            desc = "Allows to add extra graphical debug information",
            extra = "Controlled via 'overlay' command",
            categories = {COMMAND, SCARPET},
            appSource = "overlay"
    )
    public static boolean commandOverlay = true;

    @Rule(
            desc = "Turns on extra information about mobs above and around them",
            extra = "Controlled via 'ai_tracker' command",
            categories = {COMMAND, SCARPET},
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
        categories = {FEATURE, SCARPET, COMMAND}
    )
    public static String commandDraw = "true";

    @Rule(
        desc = "Enables /distance command to measure in game distance between points",
        extra = "Also enables brown carpet placement action if 'carpets' rule is turned on as well",
        appSource = "distance",
        categories = {FEATURE, SCARPET, COMMAND}
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
            categories = CREATIVE,
            validators = CarpetPermissionLevel.class,
            options = {"ops", "2", "4"}
    )
    public static String carpetCommandPermissionLevel = "ops";



    @Rule(categories = EXPERIMENTAL)
    public static boolean superSecretSetting = false;

    @Rule(
            options = {"1", "40", "80", "72000"},
            categories = CREATIVE,
            strict = false,
            validators = OneHourMaxDelayLimit.class
    )
    public static int portalCreativeDelay = 1;

    @Rule(
            options = {"1", "40", "80", "72000"},
            categories = SURVIVAL,
            strict = false,
            validators = OneHourMaxDelayLimit.class
    )
    public static int portalSurvivalDelay = 80;


    private static class OneHourMaxDelayLimit extends Validator<Integer> {
        @Override public Integer validate(CommandSourceStack source, CarpetRule<Integer> currentRule, Integer newValue, String string) {
            return (newValue > 0 && newValue <= 72000) ? newValue : null;
        }
        @Override
        public String description() { return "You must choose a value from 1 to 72000";}
    }

    @Rule(categories = {RuleCategory.BUGFIX, SURVIVAL})
    public static boolean ctrlQCraftingFix = false;

    @Rule(categories = {SURVIVAL, FEATURE})
    public static boolean persistentParrots = false;

    /*@Rule(
            desc = "Mobs growing up won't glitch into walls or go through fences",
            categories = BUGFIX,
            validators = Validator.WIP.class
    )
    public static boolean growingUpWallJump = false;

    @Rule(
            desc = "Won't let mobs glitch into blocks when reloaded.",
            extra = "Can cause slight differences in mobs behaviour",
            categories = {BUGFIX, EXPERIMENTAL},
            validators = Validator.WIP.class
    )
    public static boolean reloadSuffocationFix = false;
    */

    @Rule(categories = CREATIVE )
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
            validators = StackableShulkerBoxValidator.class,
            options = {"false", "true", "16"},
            strict = false,
            categories = {SURVIVAL, FEATURE}
    )
    public static String stackableShulkerBoxes = "false";
    public static int shulkerBoxStackSize = 1; // Referenced from Carpet extra

    @Rule(categories = {CREATIVE, TNT} )
    public static boolean explosionNoBlockDamage = false;

    @Rule(categories = {SURVIVAL, FEATURE})
    public static boolean xpFromExplosions = false;

    @Rule(categories = {CREATIVE, TNT} )
    public static boolean tntPrimerMomentumRemoved = false;

    @Rule(categories = TNT)
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

    @Rule(categories = TNT, options = "-1", strict = false,
            validators = {CheckOptimizedTntEnabledValidator.class, TNTRandomRangeValidator.class})
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

    @Rule(categories = TNT, options = "-1", strict = false,
            validators = TNTAngleValidator.class)
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

    @Rule(categories = TNT )
    public static boolean mergeTNT = false;

    @Rule(categories = {EXPERIMENTAL, OPTIMIZATION})
    public static boolean fastRedstoneDust = false;

    @Rule(categories = FEATURE)
    public static boolean huskSpawningInTemples = false;

    @Rule(categories = FEATURE)
    public static boolean shulkerSpawningInEndCities = false;

    @Rule(categories = FEATURE)
    public static boolean piglinsSpawningInBastions = false;

    @Rule(categories = {CREATIVE, TNT} )
    public static boolean tntDoNotUpdate = false;

    @Rule(categories = {CREATIVE, SURVIVAL})
    public static boolean antiCheatDisabled = false;

    private static class QuasiConnectivityValidator extends Validator<Integer> {

        @Override
        public Integer validate(CommandSourceStack source, CarpetRule<Integer> changingRule, Integer newValue, String userInput) {
            int minRange = 0;
            int maxRange = 1;

            if (source == null) {
                maxRange = Integer.MAX_VALUE;
            } else {
                for (Level level : source.getServer().getAllLevels()) {
                    maxRange = Math.max(maxRange, level.getHeight() - 1);
                }
            }

            return (newValue >= minRange && newValue <= maxRange) ? newValue : null;
        }
    }

    @Rule(
        categories = CREATIVE,
        validators = QuasiConnectivityValidator.class
    )
    public static int quasiConnectivity = 1;

    @Rule(categories = {CREATIVE, SURVIVAL, FEATURE})
    public static boolean flippinCactus = false;

    @Rule(categories = {COMMAND, CREATIVE, FEATURE})
    public static boolean hopperCounters = false;

    @Rule(categories = FEATURE)
    public static boolean movableAmethyst = false;

    @Rule(categories = FEATURE )
    public static boolean renewableSponges = false;

    @Rule(categories = {EXPERIMENTAL, FEATURE} )
    public static boolean movableBlockEntities = false;

    public enum ChainStoneMode {
        TRUE, FALSE, STICK_TO_ALL;
        public boolean enabled() {
            return this != FALSE;
        }
    }

    @Rule(
            categories = {EXPERIMENTAL, FEATURE},
            options = {"true", "false", "stick_to_all"}
    )
    public static ChainStoneMode chainStone = ChainStoneMode.FALSE;

    @Rule(categories = FEATURE)
    public static boolean desertShrubs = false;

    @Rule(categories = FEATURE )
    public static boolean silverFishDropGravel = false;

    @Rule(categories = CREATIVE )
    public static boolean summonNaturalLightning = false;

    @Rule(categories = COMMAND)
    public static String commandSpawn = "ops";

    @Rule(categories = COMMAND)
    public static String commandTick = "ops";

    @Rule(categories = COMMAND)
    public static String commandProfile = "true";

    @Rule(
            options = {"2", "4"},
            categories = CREATIVE
    )
    public static int perfPermissionLevel = 4;

    @Rule(categories = COMMAND)
    public static String commandLog = "true";

    @Rule(
            categories = {CREATIVE, SURVIVAL},
            options = {"none", "tps", "mobcaps,tps"},
            strict = false
    )
    public static String defaultLoggers = "none";

    @Rule(categories = COMMAND)
    public static String commandDistance = "true";

    @Rule(categories = COMMAND)
    public static String commandInfo = "true";

    @Rule(categories = COMMAND)
    public static String commandPerimeterInfo = "true";

    @Rule(categories = COMMAND)
    public static String commandDraw = "ops";


    @Rule(categories = {COMMAND, SCARPET})
    public static String commandScript = "true";

    private static class ModulePermissionLevel extends Validator<String> {
        @Override public String validate(CommandSourceStack source, CarpetRule<String> currentRule, String newValue, String string) {
            int permissionLevel = switch (newValue) {
                    case "false" -> 0;
                    case "true", "ops" -> 2;
                    case "0", "1", "2", "3", "4" -> Integer.parseInt(newValue);
                    default -> throw new IllegalArgumentException(); // already checked by previous validator
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
            categories = {SCARPET},
            options = {"ops", "0", "1", "2", "3", "4"},
            validators = {Validators.CommandLevel.class, ModulePermissionLevel.class}
    )
    public static String commandScriptACE = "ops";

    @Rule(categories = SCARPET)
    public static boolean scriptsAutoload = true;

    @Rule(categories = SCARPET)
    public static boolean scriptsDebugging = false;

    @Rule(categories = SCARPET)
    public static boolean scriptsOptimization = true;

    @Rule(
            categories = SCARPET,
            strict = false,
            validators = Carpet.ScarpetAppStoreValidator.class
    )
    public static String scriptsAppStore = "gnembon/scarpet/contents/programs";

    @Rule(categories = COMMAND)
    public static String commandPlayer = "ops";

    @Rule(categories = COMMAND)
    public static boolean allowSpawningOfflinePlayers = true;

    @Rule(categories = COMMAND)
    public static String commandTrackAI = "ops";

    @Rule(categories = SURVIVAL)
    public static boolean carpets = false;

    @Rule(categories = SURVIVAL)
    public static boolean missingTools = false;

    @Rule(categories = CREATIVE)
    public static boolean fillUpdates = true;

    @Rule(categories = CREATIVE)
    public static boolean interactionUpdates = true;

    @Rule(categories = CREATIVE)
    public static boolean liquidDamageDisabled = false;

    @Rule(categories = {CREATIVE, SURVIVAL, CLIENT})
    public static boolean smoothClientAnimations;

    private static class PushLimitLimits extends Validator<Integer> {
        @Override public Integer validate(CommandSourceStack source, CarpetRule<Integer> currentRule, Integer newValue, String string) {
            return (newValue>0 && newValue <= 1024) ? newValue : null;
        }
        @Override
        public String description() { return "You must choose a value from 1 to 1024";}
    }
    @Rule(
            options = {"10", "12", "14", "100"},
            categories = CREATIVE,
            strict = false,
            validators = PushLimitLimits.class
    )
    public static int pushLimit = PistonStructureResolver.MAX_PUSH_DEPTH;

    @Rule(
            options = {"9", "15", "30"},
            categories = CREATIVE,
            strict = false,
            validators = PushLimitLimits.class
    )
    public static int railPowerLimit = 9;

    private static class FillLimitMigrator extends Validator<Integer>
    {
        @Override
        public Integer validate(CommandSourceStack source, CarpetRule<Integer> changingRule, Integer newValue, String userInput)
        {
            if (source != null && source.getServer().overworld() != null)
            {
                GameRules.IntegerValue gamerule = source.getServer().getGameRules().getRule(GameRules.RULE_COMMAND_MODIFICATION_BLOCK_LIMIT);
                if (gamerule.get() != newValue)
                {
                    if (newValue == 32768 && changingRule.value() == newValue) // migration call, gamerule is different, update rule
                    {
                        Messenger.m(source, "g Syncing fillLimit rule with gamerule");
                        newValue = gamerule.get();
                    } else if (newValue != 32768 && gamerule.get() == 32768)
                    {
                        Messenger.m(source, "g Migrated value of fillLimit carpet rule to commandModificationBlockLimit gamerule");
                        gamerule.set(newValue, source.getServer());
                    }
                }
            }
            return newValue;
        }
        @Override
        public String description() { return "The value of this rule will be migrated to the gamerule";}
    }

    @Rule(
            options = {"32768", "250000", "1000000"},
            categories = CREATIVE,
            strict = false,
            validators = FillLimitMigrator.class
    )
    public static int fillLimit = 32768;

    private static class ForceloadLimitValidator extends Validator<Integer>
    {
        @Override
        public Integer validate(CommandSourceStack source, CarpetRule<Integer> currentRule, Integer newValue, String string)
        {
            return (newValue > 0 && newValue <= 20000000) ? newValue : null;
        }

        @Override
        public String description() { return "You must choose a value from 1 to 20M";}
    }
    @Rule(
            options = {"256"},
            categories = CREATIVE,
            strict = false,
            validators = ForceloadLimitValidator.class
    )
    public static int forceloadLimit = 256;

    @Rule(
            options = {"0", "1", "20"},
            categories = OPTIMIZATION,
            strict = false,
            validators = Validators.NonNegativeNumber.class
    )
    public static int maxEntityCollisions = 0;

    @Rule(
            options = {"0", "12", "20", "40"},
            categories = CREATIVE,
            strict = false,
            validators = Validators.NonNegativeNumber.class
    )
    public static int pingPlayerListLimit = 12;
    /*

    @Rule(
            desc = "fixes water performance issues",
            categories = OPTIMIZATION,
            validators = Validator.WIP.class
    )
    public static boolean waterFlow = true;
    */

    @Rule(
            options = "_",
            strict = false,
            categories = CREATIVE
    )
    public static String customMOTD = "_";

    @Rule(categories = {FEATURE, DISPENSER})
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
            options = {"0", "12", "16", "32"},
            categories = CREATIVE,
            strict = false,
            validators = ViewDistanceValidator.class
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
            options = {"0", "12", "16", "32"},
            categories = CREATIVE,
            strict = false,
            validators = SimulationDistanceValidator.class
    )
    public static int simulationDistance = 0;

    public static class ChangeSpawnChunksValidator extends Validator<Integer> {
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
            ServerLevel currentOverworld = source.getServer().overworld();
            if (currentOverworld != null)
            {
                SpawnChunks.changeSpawnSize(currentOverworld, newValue);
            }
            return newValue;
        }
    }
    @Rule(
            categories = CREATIVE,
            strict = false,
            options = {"0", "11"},
            validators = ChangeSpawnChunksValidator.class
    )
    public static int spawnChunksSize = MinecraftServer.START_CHUNK_RADIUS;

    public static class LightBatchValidator extends Validator<Integer> {
        public static void applyLightBatchSizes(MinecraftServer server, int maxBatchSize)
        {
            for (ServerLevel world : server.getAllLevels())
            {
                //world.getChunkSource().getLightEngine().setTaskPerBatch(maxBatchSize);
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
            
            applyLightBatchSizes(source.getServer(), newValue); // Apply new settings
            
            return newValue;
        }
    }
    
    @Rule(
            categories = {EXPERIMENTAL, OPTIMIZATION},
            strict = false,
            options = {"5", "50", "100", "200"},
            validators = LightBatchValidator.class
    )
    public static int lightEngineMaxBatchSize = 5;

    public enum RenewableCoralMode {
        FALSE,
        EXPANDED,
        TRUE;
    }
    @Rule(categories = FEATURE)
    public static RenewableCoralMode renewableCoral = RenewableCoralMode.FALSE;

    @Rule(categories = FEATURE)
    public static boolean renewableBlackstone = false;

    @Rule(categories = FEATURE)
    public static boolean renewableDeepslate = false;

    @Rule(categories = RuleCategory.BUGFIX)
    public static boolean placementRotationFix = false;

    @Rule(categories = OPTIMIZATION)
    public static boolean lagFreeSpawning = false;

    @Rule(categories = CREATIVE)
    public static boolean moreBlueSkulls = false;

    @Rule(categories = CLIENT)
    public static boolean fogOff = false;

    @Rule(categories = {CREATIVE, CLIENT})
    public static boolean creativeNoClip = false;
    public static boolean isCreativeFlying(Entity entity)
    {
        // #todo replace after merger to 1.17
        return CarpetSettings.creativeNoClip && entity instanceof Player p && p.isCreative() && p.getAbilities().flying;
    }

    @Rule(
            categories = {CREATIVE, CLIENT},
            strict = false,
            validators = Validators.NonNegativeNumber.class
    )
    public static double creativeFlySpeed = 1.0;

    @Rule(
            categories = {CREATIVE, CLIENT},
            strict = false,
            validators = Validators.Probablity.class
    )
    public static double creativeFlyDrag = 0.09;

    @Rule(categories = {SURVIVAL, CLIENT})
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
            options = {"48", "96", "192", "256"},
            categories = CREATIVE,
            validators = StructureBlockLimitValidator.class,
            strict = false
    )
    public static int structureBlockLimit = StructureBlockEntity.MAX_SIZE_PER_AXIS;

    public static class StructureBlockIgnoredValidator extends Validator<String> {
        @Override
        public String validate(CommandSourceStack source, CarpetRule<String> currentRule, String newValue, String string) {
            if (source == null) return newValue; // closing or sync
            Optional<Block> ignoredBlock = source.registryAccess().registryOrThrow(Registries.BLOCK).getOptional(ResourceLocation.tryParse(newValue));
            if (!ignoredBlock.isPresent()) {
                Messenger.m(source, "r Unknown block '" + newValue + "'.");
                return null;
            }
            structureBlockIgnoredBlock = ignoredBlock.get();
            return newValue;
        }
    }
    @Rule(
            options = {"minecraft:structure_void", "minecraft:air"},
            categories = CREATIVE,
            validators = StructureBlockIgnoredValidator.class,
            strict = false
    )
    public static String structureBlockIgnored = "minecraft:structure_void";

    @Rule(
            options = {"96", "192", "2048"},
            categories = {CREATIVE, CLIENT},
            strict = false,
            validators = Validators.NonNegativeNumber.class
    )
    public static int structureBlockOutlineDistance = 96;

    @Rule(categories = BUGFIX)
    public static boolean lightningKillsDropsFix = false;

    @Rule(
            categories = CREATIVE,
            options = {"-1","0","10","50"},
            strict = false,
            validators = UpdateSuppressionBlockModes.class
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

    @Rule(categories = {CREATIVE, FEATURE})
    public static boolean creativePlayersLoadChunks = true;

    @Rule(
            options = {"8", "16", "32"},
            categories = CREATIVE,
            strict = false,
            validators = PushLimitLimits.class
    )
    public static int sculkSensorRange = 8;

    /**
     * Listener, we need to update world borders to change whether
     * they are currently moving in real time or in game time.
     */
    private static class WorldBorderValidator extends Validator<Boolean>
    {
        @Override
        public Boolean validate(CommandSourceStack source, CarpetRule<Boolean> changingRule, Boolean newValue, String userInput)
        {
            if (changingRule.value() ^ newValue)
            {
                // Needed for the update
                tickSyncedWorldBorders = newValue;
                MinecraftServer server = CarpetServer.minecraft_server;
                if (server == null)
                {
                    return newValue;
                }
                for (ServerLevel level : server.getAllLevels())
                {
                    WorldBorder worldBorder = level.getWorldBorder();
                    if (worldBorder.getStatus() != BorderStatus.STATIONARY)
                    {
                        double from = worldBorder.getSize();
                        double to = worldBorder.getLerpTarget();
                        long time = worldBorder.getLerpRemainingTime();
                        worldBorder.lerpSizeBetween(from, to, time);
                    }
                }
            }
            return newValue;
        }
    }

    @Rule(
            categories = FEATURE,
            validators = WorldBorderValidator.class
    )
    public static boolean tickSyncedWorldBorders = false;

    public enum FungusGrowthMode {
        FALSE, RANDOM, ALL;
    }

    // refers to "[MC-215169](https://bugs.mojang.com/browse/MC-215169)." - unconfirmed yet that its a java bug
    @Rule(categories = {SURVIVAL, FEATURE})
    public static FungusGrowthMode thickFungusGrowth = FungusGrowthMode.FALSE;
}
