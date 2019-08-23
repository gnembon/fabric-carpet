package carpet.settings;

import carpet.CarpetServer;
import carpet.utils.Messenger;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Unit;
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

public class CarpetSettings
{
    public static final String carpetVersion = "1.0.4-v190823";
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
            desc = "Portals won't let a creative player go through instantly",
            extra = "Holding obsidian in either hand won't let you through at all",
            category = CREATIVE
    )
    public static boolean portalCreativeDelay = false;

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

    @Rule( desc = "Explosions won't destroy blocks", category = CREATIVE )
    public static boolean explosionNoBlockDamage = false;

    @Rule( desc = "Removes random TNT momentum when primed", category = CREATIVE )
    public static boolean tntPrimerMomentumRemoved = false;

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

    @Rule( desc = "TNT doesn't update when placed against a power source", category = CREATIVE )
    public static boolean tntDoNotUpdate = false;

    @Rule(
            desc = "Prevents players from rubberbanding when moving too fast",
            extra = {"... or being kicked out for 'flying'","Puts more trust in clients positioning"},
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

    @Rule( desc = "Pistons can push tile entities, like hoppers, chests etc.", category = {EXPERIMENTAL, FEATURE} )
    public static boolean movableTileEntities = false;

    @Rule( desc = "Saplings turn into dead shrubs in hot climates and no water access", category = FEATURE )
    public static boolean desertShrubs = false;

    @Rule( desc = "Silverfish drop a gravel item when breaking out of a block", category = FEATURE )
    public static boolean silverFishDropGravel = false;

    @Rule( desc = "summoning a lightning bolt has all the side effects of natural lightning", category = CREATIVE )
    public static boolean summonNaturalLightning = false;

    @Rule(desc = "Enables /spawn command for spawn tracking", category = COMMAND)
    public static boolean commandSpawn = true;

    @Rule(desc = "Enables /tick command to control game clocks", category = COMMAND)
    public static boolean commandTick = true;

    @Rule(
            desc = "Enables /log command to monitor events in the game via chat and overlays",
            category = COMMAND,
            validate = Validator.WIP.class
    )
    public static boolean commandLog = true;

    @Rule(
            desc = "Enables /distance command to measure in game distance between points",
            extra = "Also enables brown carpet placement action if 'carpets' rule is turned on as well",
            category = COMMAND
    )
    public static boolean commandDistance = true;

    @Rule(
            desc = "Enables /info command for blocks",
            extra = {
                    "Also enables gray carpet placement action",
                    "if 'carpets' rule is turned on as well"
            },
            category = COMMAND
    )
    public static boolean commandInfo = true;

    @Rule(
            desc = "Enables /c and /s commands to quickly switch between camera and survival modes",
            extra = "/c and /s commands are available to all players regardless of their permission levels",
            category = COMMAND
    )
    public static boolean commandCameramode = true;

    @Rule(
            desc = "Enables /perimeterinfo command",
            extra = "... that scans the area around the block for potential spawnable spots",
            category = COMMAND
    )
    public static boolean commandPerimeterInfo = true;

    @Rule(desc = "Enables /draw commands", extra = "... allows for drawing simple shapes", category = COMMAND)
    public static boolean commandDraw = true;

    @Rule(desc = "Enables /script command", extra = "An in-game scripting API for Scarpet programming language", category = COMMAND)
    public static boolean commandScript = true;

    @Rule(desc = "Enables /player command to control/spawn players", category = COMMAND)
    public static boolean commandPlayer = true;

    @Rule(desc = "Allows to track mobs AI via /track command", category = COMMAND)
    public static boolean commandTrackAI = true;

    @Rule(desc = "Placing carpets may issue carpet commands for non-op players", category = SURVIVAL)
    public static boolean carpets = false;

    @Rule(desc = "Pistons, Glass and Sponge can be broken faster with their appropriate tools", category = SURVIVAL)
    public static boolean missingTools = false;

    @Rule(desc = "Alternative, persistent caching strategy for nether portals", category = {SURVIVAL, CREATIVE})
    public static boolean portalCaching = false;

    @Rule(desc = "fill/clone/setblock and structure blocks cause block updates", category = CREATIVE)
    public static boolean fillUpdates = true;

    @Rule(desc = "smooth client animations with low tps settings", extra = "works only in SP, and will slow down players", category = CREATIVE)
    public static boolean smoothClientAnimations;

    @Rule(
            desc="Fixes mining ghost blocks by trusting clients with block breaking",
            extra="Increases player allowed mining distance to 32 blocks",
            category = SURVIVAL
    )
    public static boolean miningGhostBlockFix;

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
            validate = PushLimitLimits.class
    )
    public static int pushLimit = 12;

    @Rule(
            desc = "Customizable powered rail power range",
            options = {"9", "15", "30"},
            category = CREATIVE,
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
            validate = FillLimitLimits.class
    )
    public static int fillLimit = 32768;

    @Rule(
            desc = "Customizable maximal entity collision limits, 0 for no limits",
            options = {"0", "1", "20"},
            category = OPTIMIZATION,
            validate = Validator.NONNEGATIVE_NUMBER.class
    )
    public static int maxEntityCollisions = 0;

    /*
    @Rule(
            desc = "Fix for piston ghost blocks",
            category = BUGFIX,
            validate = Validator.WIP.class
    )
    public static boolean pistonGhostBlocksFix = true;

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
            category = CREATIVE
    )
    public static String customMOTD = "_";

    @Rule(desc = "Cactus in dispensers rotates blocks.", extra = "Rotates block anti-clockwise if possible", category = FEATURE)
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
                ChunkPos centerChunk = new ChunkPos(overworld.getSpawnPos());
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

    private static class KelpLimit extends Validator<Integer>
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
            validate = KelpLimit.class
    )
    public static int kelpGenerationGrowthLimit = 25;

    @Rule(desc = "Coral structures will grow with bonemeal from coral plants", category = FEATURE)
    public static boolean renewableCoral = false;

    @Rule(desc = "fixes block placement rotation issue when player rotates quickly while placing blocks", category = BUGFIX)
    public static boolean placementRotationFix = false;

    @Rule(
            desc = "Fixes leads breaking/becoming invisible in unloaded chunks",
            extra = "You may still get visibly broken leash links on the client side, but server side the link is still there.",
            category = BUGFIX
    )
    public static boolean leadFix = false;

    @Rule(desc = "Spawning requires much less CPU and Memory", category = OPTIMIZATION)
    public static boolean lagFreeSpawning = false;

    @Rule(
            desc = "Prevents horses and other mobs to wander into the distance after dismounting",
            extra = "Fixes issues with various Joergens wandering off and disappearing client-side",
            category = BUGFIX
    )
    public static boolean horseWanderingFix = false;
}
