package carpet;

import carpet.settings.ParsedRule;
import carpet.settings.Rule;
import carpet.settings.Validator;
import carpet.utils.Messenger;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static carpet.settings.RuleCategory.BUGFIX;
import static carpet.settings.RuleCategory.COMMAND;
import static carpet.settings.RuleCategory.CREATIVE;
import static carpet.settings.RuleCategory.EXPERIMENTAL;
import static carpet.settings.RuleCategory.FEATURE;
import static carpet.settings.RuleCategory.OPTIMIZATION;
import static carpet.settings.RuleCategory.SURVIVAL;
import static carpet.settings.RuleCategory.TNT;
import static carpet.settings.RuleCategory.DISPENSER;

@SuppressWarnings("CanBeFinal")
public class CarpetSettings
{
    public static final String carpetVersion = "1.3.10+v200212";
    public static final Logger LOG = LogManager.getLogger();
    public static boolean skipGenerationChecks = false;
    public static boolean impendingFillSkipUpdates = false;
    public static Box currentTelepotingEntityBox = null;
    public static Vec3d fixedPosition = null;

    @Rule(
            desc = "Nether portals correctly place entities going through",
            extra = "Entities shouldn't suffocate in obsidian",
            category = BUGFIX
    )
    public static boolean portalSuffocationFix = false;

    @Rule(desc = "Gbhs sgnf sadsgras fhskdpri!", category = EXPERIMENTAL)
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
        @Override public Integer validate(ServerCommandSource source, ParsedRule<Integer> currentRule, Integer newValue, String string) {
            return (newValue > 0 && newValue <= 72000) ? newValue : null;
        }
        @Override
        public String description() { return "You must choose a value from 1 to 72000";}
    }

    @Rule(desc = "Dropping entire stacks works also from on the crafting UI result slot", category = {BUGFIX, SURVIVAL})
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

    @Rule( desc = "XP orbs combine with other into bigger orbs", category = FEATURE )
    public static boolean combineXPOrbs = false;

    @Rule(
            desc = "Empty shulker boxes can stack to 64 when dropped on the ground",
            extra = "To move them around between inventories, use shift click to move entire stacks",
            category = {SURVIVAL, FEATURE}
    )
    public static boolean stackableShulkerBoxes = false;

    @Rule( desc = "Explosions won't destroy blocks", category = {CREATIVE, TNT} )
    public static boolean explosionNoBlockDamage = false;

    @Rule( desc = "Removes random TNT momentum when primed", category = {CREATIVE, TNT} )
    public static boolean tntPrimerMomentumRemoved = false;

    @Rule( desc = "TNT causes less lag when exploding in the same spot and in liquids", category = TNT)
    public static boolean optimizedTNT = false;

    private static class CheckOptimizedTntEnabledValidator<T> extends Validator<T>
    {
        @Override
        public T validate(ServerCommandSource source, ParsedRule<T> currentRule, T newValue, String string) {
            return optimizedTNT || currentRule.defaultValue.equals(newValue) ? newValue : null;
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
        public Double validate(ServerCommandSource source, ParsedRule<Double> currentRule, Double newValue, String string) {
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
        public Double validate(ServerCommandSource source, ParsedRule<Double> currentRule, Double newValue, String string) {
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
            extra = "by Theosib",
            category = {EXPERIMENTAL, OPTIMIZATION}
    )
    public static boolean fastRedstoneDust = false;

    @Rule(desc = "Only husks spawn in desert temples", category = FEATURE)
    public static boolean huskSpawningInTemples = false;

    @Rule( desc = "Shulkers will respawn in end cities", category = FEATURE )
    public static boolean shulkerSpawningInEndCities = false;

    @Rule(desc = "Entities pushed or moved into unloaded chunks no longer disappear", category = {EXPERIMENTAL, BUGFIX})
    public static boolean unloadedEntityFix = false;

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

    @Rule( desc = "Guardians turn into Elder Guardian when struck by lightning", category = FEATURE )
    public static boolean renewableSponges = false;

    @Rule( desc = "Pistons can push block entities, like hoppers, chests etc.", category = {EXPERIMENTAL, FEATURE} )
    public static boolean movableBlockEntities = false;

    /*@Rule(
            desc = "Orange stained glass behaves like slime, but doesn't stick to it",
            extra = "Only available for 1.14. in 1.15 use honey instead",
            category = {EXPERIMENTAL, FEATURE}
    )
    public static boolean honeySlime = false;*/

    @Rule( desc = "Saplings turn into dead shrubs in hot climates and no water access", category = FEATURE )
    public static boolean desertShrubs = false;

    @Rule( desc = "Silverfish drop a gravel item when breaking out of a block", category = FEATURE )
    public static boolean silverFishDropGravel = false;

    @Rule( desc = "summoning a lightning bolt has all the side effects of natural lightning", category = CREATIVE )
    public static boolean summonNaturalLightning = false;

    @Rule(desc = "Enables /spawn command for spawn tracking", category = COMMAND)
    public static String commandSpawn = "true";

    @Rule(desc = "Enables /tick command to control game clocks", category = COMMAND)
    public static String commandTick = "true";

    @Rule(
            desc = "Enables /profile command to monitor game performance",
            extra = "subset of /tick command capabilities",
            category = COMMAND
    )
    public static String commandProfile = "true";

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
            desc = "Enables /c and /s commands to quickly switch between camera and survival modes",
            extra = "/c and /s commands are available to all players regardless of their permission levels",
            category = COMMAND
    )
    public static String commandCameramode = "true";

    @Rule(
            desc = "Enables /perimeterinfo command",
            extra = "... that scans the area around the block for potential spawnable spots",
            category = COMMAND
    )
    public static String commandPerimeterInfo = "true";

    @Rule(desc = "Enables /draw commands", extra = "... allows for drawing simple shapes", category = COMMAND)
    public static String commandDraw = "true";

    @Rule(desc = "Enables /script command", extra = "An in-game scripting API for Scarpet programming language", category = COMMAND)
    public static boolean commandScript = true;

    @Rule(
            desc = "Scarpet script from world files will autoload on server/world start ",
            extra = "if /script is enabled",
            category = CREATIVE
    )
    public static boolean scriptsAutoload = false;

    @Rule(desc = "Enables /player command to control/spawn players", category = COMMAND)
    public static String commandPlayer = "true";

    @Rule(desc = "Allows to track mobs AI via /track command", category = COMMAND)
    public static String commandTrackAI = "true";

    @Rule(desc = "Placing carpets may issue carpet commands for non-op players", category = SURVIVAL)
    public static boolean carpets = false;

    @Rule(desc = "Pistons, Glass and Sponge can be broken faster with their appropriate tools", category = SURVIVAL)
    public static boolean missingTools = false;

    //@Rule(desc = "Alternative, persistent caching strategy for nether portals", category = {SURVIVAL, CREATIVE})
    //public static boolean portalCaching = false; // ineffective in 1.15

    @Rule(desc = "fill/clone/setblock and structure blocks cause block updates", category = CREATIVE)
    public static boolean fillUpdates = true;

    @Rule(desc = "placing blocks cause block updates", category = CREATIVE)
    public static boolean interactionUpdates = true;

    @Rule(desc = "smooth client animations with low tps settings", extra = "works only in SP, and will slow down players", category = CREATIVE)
    public static boolean smoothClientAnimations;

    //@Rule(
    //        desc="Fixes mining ghost blocks by trusting clients with block breaking",
    //        extra="Increases player allowed mining distance to 32 blocks",
    //        category = SURVIVAL
    //)
    //public static boolean miningGhostBlockFix = false;

    private static class PushLimitLimits extends Validator<Integer> {
        @Override public Integer validate(ServerCommandSource source, ParsedRule<Integer> currentRule, Integer newValue, String string) {
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
    public static int pushLimit = 12;

    @Rule(
            desc = "Customizable powered rail power range",
            options = {"9", "15", "30"},
            category = CREATIVE,
            strict = false,
            validate = PushLimitLimits.class
    )
    public static int railPowerLimit = 9;

    private static class FillLimitLimits extends Validator<Integer> {
        @Override public Integer validate(ServerCommandSource source, ParsedRule<Integer> currentRule, Integer newValue, String string) {
            return (newValue>0 && newValue < 20000000) ? newValue : null;
        }
        @Override
        public String description() { return "You must choose a value from 1 to 20M";}
    }
    @Rule(
            desc = "Customizable fill/clone volume limit",
            options = {"32768", "250000", "1000000"},
            category = CREATIVE,
            strict = false,
            validate = FillLimitLimits.class
    )
    public static int fillLimit = 32768;


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
            validate = Validator.NONNEGATIVE_NUMBER.class
    )
    public static int maxEntityCollisions = 0;

    /*

    @Rule(
            desc = "fixes water performance issues",
            category = OPTIMIZATION,
            validate = Validator.WIP.class
    )
    public static boolean waterFlow = true;
    */

    @Rule(desc = "One player is required on the server to cause night to pass", category = SURVIVAL)
    public static boolean onePlayerSleeping = false;

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
        @Override public Integer validate(ServerCommandSource source, ParsedRule<Integer> currentRule, Integer newValue, String string)
        {
            if (currentRule.get().equals(newValue))
            {
                return newValue;
            }
            if (newValue < 0 || newValue > 32)
            {
                Messenger.m(source, "r view distance has to be between 0 and 32");
                return null;
            }
            MinecraftServer server = source.getMinecraftServer();

            if (server.isDedicated())
            {
                int vd = (newValue >= 2)?newValue:((DedicatedServer) server).getProperties().viewDistance;
                if (vd != CarpetServer.minecraft_server.getPlayerManager().getViewDistance())
                    CarpetServer.minecraft_server.getPlayerManager().setViewDistance(vd);
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

    private static class DisableSpawnChunksValidator extends Validator<Boolean> {
        @Override public Boolean validate(ServerCommandSource source, ParsedRule<Boolean> currentRule, Boolean newValue, String string) {
            if (currentRule.get().booleanValue() == newValue.booleanValue())
            {
                //must been some startup thing
                return newValue;
            }
            ServerWorld overworld = CarpetServer.minecraft_server.getWorld(DimensionType.OVERWORLD);
            if (overworld != null) {
                ChunkPos centerChunk = new ChunkPos(new BlockPos(
                        overworld.getLevelProperties().getSpawnX(),
                        overworld.getLevelProperties().getSpawnY(),
                        overworld.getLevelProperties().getSpawnZ()
                ));
                ServerChunkManager chunkManager = (ServerChunkManager) overworld.getChunkManager();

                chunkManager.removeTicket(ChunkTicketType.START, centerChunk, 11, Unit.INSTANCE);
                if (!newValue)
                {
                    chunkManager.addTicket(ChunkTicketType.START, centerChunk, 11, Unit.INSTANCE);
                }
            }
            return newValue;
        }
    }
    @Rule(
            desc = "Allows spawn chunks to unload",
            category = CREATIVE,
            validate = DisableSpawnChunksValidator.class
    )
    public static boolean disableSpawnChunks = false;

    /*private static class KelpLimit extends Validator<Integer>
    {
        @Override public Integer validate(ServerCommandSource source, ParsedRule<Integer> currentRule, Integer newValue, String string) {
            return (newValue>=0 && newValue <=25)?newValue:null;
        }
        @Override
        public String description() { return "You must choose a value from 0 to 25. 25 and all natural kelp can grow 25 blocks, choose 0 to make all generated kelp not to grow";}
    }
    @Rule(
            desc = "limits growth limit of newly naturally generated kelp to this amount of blocks",
            options = {"0", "2", "25"},
            category = FEATURE,
            strict = false,
            validate = KelpLimit.class
    )
    public static int kelpGenerationGrowthLimit = 25;*/

    @Rule(desc = "Coral structures will grow with bonemeal from coral plants", category = FEATURE)
    public static boolean renewableCoral = false;

    @Rule(desc = "fixes block placement rotation issue when player rotates quickly while placing blocks", category = BUGFIX)
    public static boolean placementRotationFix = false;

    @Rule(
            desc = "Fixes leads breaking/becoming invisible in unloaded chunks",
            extra = "You may still get visibly broken leash links on the client side, but server side the link is still there.",
            category = BUGFIX
    )// needs checkfix for 1.15
    public static boolean leadFix = false;

    @Rule(desc = "Spawning requires much less CPU and Memory", category = OPTIMIZATION)
    public static boolean lagFreeSpawning = false;

    //@Rule(
    //        desc = "Prevents horses and other mobs to wander into the distance after dismounting",
    //        extra = "Fixes issues with various Joergens wandering off and disappearing client-side",
    //        category = BUGFIX
    //)
    //public static boolean horseWanderingFix = false;
    
    @Rule(
            desc = "Allows structure mobs to spawn in flat worlds",
            category = {EXPERIMENTAL, CREATIVE}
    )
    public static boolean flatWorldStructureSpawning = false;

    @Rule(
            desc = "Edge cases are as frequent as common cases, for testing only!!",
            extra = {"Velocities of items from dispensers, blaze projectiles, fireworks ",
                    "Directions of fireballs, wither skulls, fishing bobbers, ",
                    "items dropped from blocks and inventories, llamas spit, triggered trap horses",
                    "Damage dealt with projectiles",
                    "Blaze aggro sensitivity",
                    "Mobs spawned follow range"
            },
            category = CREATIVE
    )
    public static boolean extremeBehaviours = false;
}
