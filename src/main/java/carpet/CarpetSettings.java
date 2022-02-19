package carpet;

import carpet.script.utils.AppStoreManager;
import carpet.settings.ParsedRule;
import carpet.settings.Rule;
import carpet.settings.SettingsManager;
import carpet.settings.Validator;
import carpet.utils.Translations;
import carpet.utils.Messenger;
import carpet.utils.SpawnChunks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Iterator;
import java.util.Locale;
import java.util.Optional;

import static carpet.settings.RuleCategory.BUGFIX;
import static carpet.settings.RuleCategory.COMMAND;
import static carpet.settings.RuleCategory.CREATIVE;
import static carpet.settings.RuleCategory.EXPERIMENTAL;
import static carpet.settings.RuleCategory.FEATURE;
import static carpet.settings.RuleCategory.OPTIMIZATION;
import static carpet.settings.RuleCategory.SURVIVAL;
import static carpet.settings.RuleCategory.TNT;
import static carpet.settings.RuleCategory.DISPENSER;
import static carpet.settings.RuleCategory.SCARPET;
import static carpet.settings.RuleCategory.CLIENT;

@SuppressWarnings("CanBeFinal")
public class CarpetSettings
{
    public static final String carpetVersion = "1.4.62+v220216";
    public static final Logger LOG = LogManager.getLogger("carpet");
    public static ThreadLocal<Boolean> skipGenerationChecks = ThreadLocal.withInitial(() -> false);
    public static ThreadLocal<Boolean> impendingFillSkipUpdates = ThreadLocal.withInitial(() -> false);
    public static int runPermissionLevel = 2;
    public static boolean doChainStone = false;
    public static boolean chainStoneStickToAll = false;
    public static Block structureBlockIgnoredBlock = Blocks.STRUCTURE_VOID;
    public static final int vanillaStructureBlockLimit = 48;
    public static int updateSuppressionBlockSetting = -1;

    private static class LanguageValidator extends Validator<String> {
        @Override public String validate(CommandSourceStack source, ParsedRule<String> currentRule, String newValue, String string) {
            if (currentRule.get().equals(newValue) || source == null)
            {
                return newValue;
            }
            if (!Translations.isValidLanguage(newValue))
            {
                Messenger.m(source, "r "+newValue+" is not a valid language");
                return null;
            }
            CarpetSettings.language = newValue;
            return newValue;
        }
    }
    @Rule(
            category = FEATURE,
            options = {"none", "zh_cn", "zh_tw"},
            strict = false,
            validate = LanguageValidator.class
    )
    public static String language = "none";

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
        @Override public String validate(CommandSourceStack source, ParsedRule<String> currentRule, String newValue, String string) {
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
            category = CREATIVE,
            validate = CarpetPermissionLevel.class,
            options = {"ops", "2", "4"}
    )
    public static String carpetCommandPermissionLevel = "ops";



    @Rule( category = EXPERIMENTAL )
    public static boolean superSecretSetting = false;

    @Rule(
            options = {"1", "40", "80", "72000"},
            category = CREATIVE,
            strict = false,
            validate = OneHourMaxDelayLimit.class
    )
    public static int portalCreativeDelay = 1;

    @Rule(
            options = {"1", "40", "80", "72000"},
            category = SURVIVAL,
            strict = false,
            validate = OneHourMaxDelayLimit.class
    )
    public static int portalSurvivalDelay = 80;


    private static class OneHourMaxDelayLimit extends Validator<Integer> {
        @Override public Integer validate(CommandSourceStack source, ParsedRule<Integer> currentRule, Integer newValue, String string) {
            return (newValue > 0 && newValue <= 72000) ? newValue : null;
        }
        @Override
        public String description() { return "You must choose a value from 1 to 72000";}
    }

    @Rule( category = {BUGFIX, SURVIVAL} )
    public static boolean ctrlQCraftingFix = false;

    @Rule( category = {SURVIVAL, FEATURE} )
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

    @Rule( category = CREATIVE )
    public static boolean xpNoCooldown = false;

    public static class StackableShulkerBoxValidator extends Validator<String> 
    {
        @Override
        public String validate(CommandSourceStack source, ParsedRule<String> currentRule, String newValue, String string)
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
            validate = StackableShulkerBoxValidator.class,
            options = {"false", "true", "16"},
            strict = false,
            category = {SURVIVAL, FEATURE}
    )
    public static String stackableShulkerBoxes = "false";
    public static int shulkerBoxStackSize = 1;

    @Rule( category = {CREATIVE, TNT} )
    public static boolean explosionNoBlockDamage = false;

    @Rule( category = {CREATIVE, TNT} )
    public static boolean tntPrimerMomentumRemoved = false;

    @Rule( category = TNT)
    public static boolean optimizedTNT = false;

    private static class CheckOptimizedTntEnabledValidator<T> extends Validator<T>
    {
        @Override
        public T validate(CommandSourceStack source, ParsedRule<T> currentRule, T newValue, String string) {
            return optimizedTNT || currentRule.defaultValue.equals(newValue) ? newValue : null;
        }

        @Override
        public String description() {
            return "optimizedTNT must be enabled";
        }
    }

    @Rule( category = TNT, options = "-1", strict = false,
            validate = {CheckOptimizedTntEnabledValidator.class, TNTRandomRangeValidator.class})
    public static double tntRandomRange = -1;

    private static class TNTRandomRangeValidator extends Validator<Double> {
        @Override
        public Double validate(CommandSourceStack source, ParsedRule<Double> currentRule, Double newValue, String string) {
            return newValue == -1 || newValue >= 0 ? newValue : null;
        }

        @Override
        public String description() {
            return "Cannot be negative, except for -1";
        }
    }

    @Rule( category = TNT, options = "-1", strict = false,
            validate = TNTAngleValidator.class)
    public static double hardcodeTNTangle = -1.0D;

    private static class TNTAngleValidator extends Validator<Double> {
        @Override
        public Double validate(CommandSourceStack source, ParsedRule<Double> currentRule, Double newValue, String string) {
            return (newValue >= 0 && newValue < Math.PI * 2) || newValue == -1 ? newValue : null;
        }

        @Override
        public String description() {
            return "Must be between 0 and 2pi, or -1";
        }
    }

    @Rule( category = TNT )
    public static boolean mergeTNT = false;

    @Rule( category = {EXPERIMENTAL, OPTIMIZATION} )
    public static boolean fastRedstoneDust = false;

    @Rule( category = FEATURE )
    public static boolean huskSpawningInTemples = false;

    @Rule( category = FEATURE )
    public static boolean shulkerSpawningInEndCities = false;

    @Rule( category = FEATURE )
    public static boolean piglinsSpawningInBastions = false;

    @Rule( category = {CREATIVE, TNT} )
    public static boolean tntDoNotUpdate = false;

    @Rule( category = {CREATIVE, SURVIVAL} )
    public static boolean antiCheatDisabled = false;

    @Rule( category = CREATIVE )
    public static boolean quasiConnectivity = true;

    @Rule( category = {CREATIVE, SURVIVAL, FEATURE} )
    public static boolean flippinCactus = false;

    @Rule( category = {COMMAND, CREATIVE, FEATURE} )
    public static boolean hopperCounters = false;

    @Rule( category = FEATURE )
    public static boolean movableAmethyst = false;

    @Rule( category = FEATURE )
    public static boolean renewableSponges = false;

    @Rule( category = {EXPERIMENTAL, FEATURE} )
    public static boolean movableBlockEntities = false;


    private static class ChainStoneSetting extends Validator<String> {
        @Override public String validate(CommandSourceStack source, ParsedRule<String> currentRule, String newValue, String string) {
            CarpetSettings.doChainStone = !newValue.toLowerCase(Locale.ROOT).equals("false");
            CarpetSettings.chainStoneStickToAll = newValue.toLowerCase(Locale.ROOT).equals("stick_to_all");

            return newValue;
        }
    }

    @Rule(
            category = {EXPERIMENTAL, FEATURE},
            options = {"true", "false", "stick_to_all"},
            validate = ChainStoneSetting.class
    )
    public static String chainStone = "false";

    @Rule( category = FEATURE )
    public static boolean desertShrubs = false;

    @Rule( category = FEATURE )
    public static boolean silverFishDropGravel = false;

    @Rule( category = CREATIVE )
    public static boolean summonNaturalLightning = false;

    @Rule( category = COMMAND )
    public static String commandSpawn = "ops";

    @Rule( category = COMMAND )
    public static String commandTick = "ops";

    @Rule( category = COMMAND )
    public static String commandProfile = "true";

    @Rule(
            options = {"2", "4"},
            category = CREATIVE
    )
    public static int perfPermissionLevel = 4;

    @Rule( category = COMMAND )
    public static String commandLog = "true";

    @Rule(
            category = {CREATIVE, SURVIVAL},
            options = {"none", "tps", "mobcaps,tps"},
            strict = false
    )
    public static String defaultLoggers = "none";

    @Rule( category = COMMAND )
    public static String commandDistance = "true";

    @Rule( category = COMMAND )
    public static String commandInfo = "true";

    @Rule( category = COMMAND)
    public static String commandPerimeterInfo = "true";

    @Rule( category = COMMAND )
    public static String commandDraw = "ops";

    @Rule( category = {COMMAND, SCARPET})
    public static String commandScript = "true";

    private static class ModulePermissionLevel extends Validator<String> {
        @Override public String validate(CommandSourceStack source, ParsedRule<String> currentRule, String newValue, String string) {
            int permissionLevel = SettingsManager.getCommandLevel(newValue);
            if (source != null && !source.hasPermission(permissionLevel))
                return null;
            CarpetSettings.runPermissionLevel = permissionLevel;
            CarpetServer.settingsManager.notifyPlayersCommandsChanged();
            return newValue;
        }
        @Override
        public String description() { return "When changing the rule, you must at least have the permission level you are trying to give it";}
    }
    @Rule(
            category = {SCARPET},
            options = {"ops", "0", "1", "2", "3", "4"},
            validate = {Validator._COMMAND_LEVEL_VALIDATOR.class, ModulePermissionLevel.class}
    )
    public static String commandScriptACE = "ops";

    @Rule( category = SCARPET )
    public static boolean scriptsAutoload = true;

    @Rule( category = SCARPET )
    public static boolean scriptsDebugging = false;

    @Rule( category = SCARPET )
    public static boolean scriptsOptimization = true;

    @Rule(
            category = SCARPET,
            strict = false,
            validate= AppStoreManager.ScarpetAppStoreValidator.class
    )
    public static String scriptsAppStore = "gnembon/scarpet/contents/programs";


    @Rule( category = COMMAND )
    public static String commandPlayer = "ops";

    @Rule( category = COMMAND )
    public static boolean allowSpawningOfflinePlayers = true;

    @Rule( category = COMMAND )
    public static String commandTrackAI = "ops";

    @Rule( category = SURVIVAL )
    public static boolean carpets = false;

    @Rule( category = SURVIVAL)
    public static boolean missingTools = false;

    @Rule( category = CREATIVE )
    public static boolean fillUpdates = true;

    @Rule( category = CREATIVE )
    public static boolean interactionUpdates = true;

    @Rule( category = CREATIVE )
    public static boolean liquidDamageDisabled = false;

    @Rule( category = {CREATIVE, SURVIVAL, CLIENT} )
    public static boolean smoothClientAnimations;

    //@Rule(
    //        desc="Fixes mining ghost blocks by trusting clients with block breaking",
    //        extra="Increases player allowed mining distance to 32 blocks",
    //        category = SURVIVAL
    //)
    //public static boolean miningGhostBlockFix = false;

    private static class PushLimitLimits extends Validator<Integer> {
        @Override public Integer validate(CommandSourceStack source, ParsedRule<Integer> currentRule, Integer newValue, String string) {
            return (newValue>0 && newValue <= 1024) ? newValue : null;
        }
        @Override
        public String description() { return "You must choose a value from 1 to 1024";}
    }
    @Rule(
            options = {"10", "12", "14", "100"},
            category = CREATIVE,
            strict = false,
            validate = PushLimitLimits.class
    )
    public static int pushLimit = 12;

    @Rule(
            options = {"9", "15", "30"},
            category = CREATIVE,
            strict = false,
            validate = PushLimitLimits.class
    )
    public static int railPowerLimit = 9;

    private static class FillLimitLimits extends Validator<Integer> {
        @Override public Integer validate(CommandSourceStack source, ParsedRule<Integer> currentRule, Integer newValue, String string) {
            return (newValue>0 && newValue <= 20000000) ? newValue : null;
        }
        @Override
        public String description() { return "You must choose a value from 1 to 20M";}
    }
    @Rule(
            options = {"32768", "250000", "1000000"},
            category = CREATIVE,
            strict = false,
            validate = FillLimitLimits.class
    )
    public static int fillLimit = 32768;


    @Rule(
            options = {"256"},
            category = CREATIVE,
            strict = false,
            validate = FillLimitLimits.class
    )
    public static int forceloadLimit = 256;

    @Rule(
            options = {"0", "1", "20"},
            category = OPTIMIZATION,
            strict = false,
            validate = Validator.NONNEGATIVE_NUMBER.class
    )
    public static int maxEntityCollisions = 0;

    @Rule(
            options = {"0", "12", "20", "40"},
            category = CREATIVE,
            strict = false,
            validate = Validator.NONNEGATIVE_NUMBER.class
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
            options = "_",
            strict = false,
            category = CREATIVE
    )
    public static String customMOTD = "_";

    @Rule( category = {FEATURE, DISPENSER} )
    public static boolean rotatorBlock = false;

    private static class ViewDistanceValidator extends Validator<Integer>
    {
        @Override public Integer validate(CommandSourceStack source, ParsedRule<Integer> currentRule, Integer newValue, String string)
        {
            if (currentRule.get().equals(newValue) || source == null)
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
            category = CREATIVE,
            strict = false,
            validate = ViewDistanceValidator.class
    )
    public static int viewDistance = 0;

    private static class SimulationDistanceValidator extends Validator<Integer>
    {
        @Override public Integer validate(CommandSourceStack source, ParsedRule<Integer> currentRule, Integer newValue, String string)
        {
            if (currentRule.get().equals(newValue) || source == null)
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
        @Override public Integer validate(CommandSourceStack source, ParsedRule<Integer> currentRule, Integer newValue, String string) {
            if (source == null) return newValue;
            if (newValue < 0 || newValue > 32)
            {
                Messenger.m(source, "r spawn chunk size has to be between 0 and 32");
                return null;
            }
            if (currentRule.get().intValue() == newValue.intValue())
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
            category = CREATIVE,
            strict = false,
            options = {"0", "11"},
            validate = ChangeSpawnChunksValidator.class
    )
    public static int spawnChunksSize = 11;

    public static class LightBatchValidator extends Validator<Integer> {
        public static void applyLightBatchSizes()
        {
            Iterator<ServerLevel> iterator = CarpetServer.minecraft_server.getAllLevels().iterator();
            
            while (iterator.hasNext()) 
            {
                ServerLevel serverWorld = iterator.next();
                serverWorld.getChunkSource().getLightEngine().setTaskPerBatch(lightEngineMaxBatchSize);
            }
        }
        @Override public Integer validate(CommandSourceStack source, ParsedRule<Integer> currentRule, Integer newValue, String string) {
            if (source == null) return newValue;
            if (newValue < 0)
            {
                Messenger.m(source, "r light batch size has to be at least 0");
                return null;
            }
            if (currentRule.get().intValue() == newValue.intValue())
            {
                //must been some startup thing
                return newValue;
            }
            if (CarpetServer.minecraft_server == null) return newValue;
          
            // Set the field before we apply.
            try
            {
                currentRule.field.set(null, newValue.intValue());
            }
            catch (IllegalAccessException e)
            {
                Messenger.m(source, "r Unable to access setting for  "+currentRule.name);
                return null;
            }
            
            applyLightBatchSizes(); // Apply new settings
            
            return newValue;
        }
    }
    
    @Rule(
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
    @Rule( category = FEATURE )
    public static RenewableCoralMode renewableCoral = RenewableCoralMode.FALSE;

    @Rule( category = FEATURE)
    public static boolean renewableBlackstone = false;

    @Rule( category = FEATURE )
    public static boolean renewableDeepslate = false;

    @Rule( category = BUGFIX )
    public static boolean placementRotationFix = false;

    @Rule( category = BUGFIX )  // needs checkfix for 1.15
    public static boolean leadFix = false;

    @Rule( category = OPTIMIZATION )
    public static boolean lagFreeSpawning = false;
    
    @Rule( category = {EXPERIMENTAL, CREATIVE} )
    public static boolean flatWorldStructureSpawning = false;

    @Rule( category = CREATIVE )
    public static boolean extremeBehaviours = false;

    @Rule( category = CLIENT )
    public static boolean fogOff = false;

    @Rule( category = {CREATIVE, CLIENT} )
    public static boolean creativeNoClip = false;
    public static boolean isCreativeFlying(Entity entity)
    {
        // #todo replace after merger to 1.17
        return CarpetSettings.creativeNoClip && entity instanceof Player && (((Player) entity).isCreative()) && ((Player) entity).getAbilities().flying;
    }


    @Rule(
            category = {CREATIVE, CLIENT},
            strict = false,
            validate = Validator.NONNEGATIVE_NUMBER.class
    )
    public static double creativeFlySpeed = 1.0;

    @Rule(
            category = {CREATIVE, CLIENT},
            strict = false,
            validate = Validator.PROBABILITY.class
    )
    public static double creativeFlyDrag = 0.09;

    @Rule( category = {SURVIVAL, CLIENT} )
    public static boolean cleanLogs = false;

    public static class StructureBlockLimitValidator extends Validator<Integer> {

        @Override public Integer validate(CommandSourceStack source, ParsedRule<Integer> currentRule, Integer newValue, String string) {
            return (newValue >= vanillaStructureBlockLimit) ? newValue : null;
        }

        @Override
        public String description() {
            return "You have to choose a value greater or equal to 48";
        }
    }
    @Rule(
            options = {"48", "96", "192", "256"},
            category = CREATIVE,
            validate = StructureBlockLimitValidator.class,
            strict = false
    )
    public static int structureBlockLimit = vanillaStructureBlockLimit;

    public static class StructureBlockIgnoredValidator extends Validator<String> {

        @Override
        public String validate(CommandSourceStack source, ParsedRule<String> currentRule, String newValue, String string) {
            Optional<Block> ignoredBlock = Registry.BLOCK.getOptional(ResourceLocation.tryParse(newValue));
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
            category = CREATIVE,
            validate = StructureBlockIgnoredValidator.class,
            strict = false
    )
    public static String structureBlockIgnored = "minecraft:structure_void";

    @Rule(
            options = {"96", "192", "2048"},
            category = {CREATIVE, CLIENT},
            strict = false,
            validate = Validator.NONNEGATIVE_NUMBER.class
    )
    public static int structureBlockOutlineDistance = 96;

    @Rule( category = {BUGFIX} )
    public static boolean lightningKillsDropsFix = false;

    @Rule(
            category = {CREATIVE, "extras"},
            options = {"false","true","1","6"},
            strict = false,
            validate = updateSuppressionBlockModes.class
    )
    public static String updateSuppressionBlock = "false";

    @Rule( category = {BUGFIX} )
    public static boolean updateSuppressionCrashFix = false;

    public static int getInteger(String s) {
        try {
            return Integer.parseInt(s);
        } catch(NumberFormatException e) {
            return -1;
        }
    }

    private static class updateSuppressionBlockModes extends Validator<String> {
        @Override
        public String validate(CommandSourceStack source, ParsedRule<String> currentRule, String newValue, String string) {
            if (!currentRule.get().equals(newValue)) {
                if (newValue.equalsIgnoreCase("false")) {
                    updateSuppressionBlockSetting = -1;
                } else if (newValue.equalsIgnoreCase("true")) {
                    updateSuppressionBlockSetting = 0;
                } else {
                    int parsedInt = getInteger(newValue);
                    if (parsedInt <= 0) {
                        updateSuppressionBlockSetting = -1;
                        return "false";
                    } else {
                        updateSuppressionBlockSetting = parsedInt;
                    }
                }
            }
            return newValue;
        }
        @Override
        public String description() {
            return "Cannot be negative, can be true, false, or # > 0";
        }
    }

    @Rule( category = {CREATIVE, FEATURE} )
    public static boolean creativePlayersLoadChunks = true;

}
