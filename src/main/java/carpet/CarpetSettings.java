package carpet;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileNotFoundException;

//import carpet.helpers.SpawnChunks;
import carpet.fakes.MinecraftServer_motdInterface;
import carpet.utils.Messenger;
import net.minecraft.server.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.dimension.DimensionType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.server.MinecraftServer;

public class CarpetSettings
{
    public static boolean locked = false;
    public static final String carpetVersion = "v19_06_04alpha";

    public static final Logger LOG = LogManager.getLogger();
    private static final Map<String, CarpetSettingEntry> settings_store;
    public static final CarpetSettingEntry FalseEntry = CarpetSettingEntry.create("void","all","Error").choices("None","");

    public static final String[] default_tags = {"tnt","fix","survival","creative", "experimental","optimizations","feature","commands"}; //tab completion only
    public static boolean skipGenerationChecks = false;
    public static boolean impendingFillSkipUpdates = false;

    static {
        settings_store = new HashMap<>();
        set_defaults();
    }

    //those don't have to mimic defaults - defaults will be '
    //static store
    public static int n_pushLimit = 12;
    public static int n_maxEntityCollisions = 0;
    public static boolean b_hopperCounters = false;
    public static boolean b_shulkerSpawningInEndCities = false;
    public static boolean b_fastRedstoneDust = false;
    public static int railPowerLimitAdjusted = 8;
    public static boolean b_disableSpawnChunks = false;
    public static boolean b_movableTileEntities = false;
    public static boolean b_huskSpawningInTemples = false;
    public static boolean b_stackableShulkerBoxes = false;
    public static boolean b_fillUpdates = true;
    public static boolean b_lagFreeSpawning = false;

    private static CarpetSettingEntry rule(String s1, String s2, String s3) { return CarpetSettingEntry.create(s1,s2,s3);}
    
    private static void set_defaults()
    {
        CarpetSettingEntry[] RuleList = new CarpetSettingEntry[] {
  rule("watchdogCrashFix", "fix", "Fixes server crashing supposedly on falling behind 60s in ONE tick, yeah bs.").
                                   extraInfo("Fixed 1.12 watchdog crash in 1.13 pre-releases, reintroduced with 1.13, GG.").wip(),
  rule("portalSuffocationFix",  "fix", "Nether portals correctly place entities going through")
                                .extraInfo("Entities shouldn't suffocate in obsidian").wip(),
  rule("superSecretSetting",    "experimental","Gbhs sgnf sadsgras fhskdpri!"),
  rule("invisibilityFix",       "fix", "Guardians honor players' invisibility effect").wip(),
  rule("portalCreativeDelay",   "creative",  "Portals won't let a creative player go through instantly")
                                .extraInfo("Holding obsidian in either hand won't let you through at all"),
  rule("ctrlQCraftingFix",      "fix survival", "Dropping entire stacks works also from on the crafting UI result slot"),
  rule("persistentParrots",     "survival feature", "Parrots don't get of your shoulders until you receive damage").wip(),
  //!rule("growingUpWallJump",     "fix", "Mobs growing up won't glitch into walls or go through fences"),
  //!rule("reloadSuffocationFix",  "fix experimental", "Won't let mobs glitch into blocks when reloaded.")
  //                              .extraInfo("Can cause slight differences in mobs behaviour"),
  rule("xpNoCooldown",          "creative", "Players absorb XP instantly, without delay"),
  rule("combineXPOrbs",         "creative", "XP orbs combine with other into bigger orbs"),
  rule("stackableShulkerBoxes", "survival", "Empty shulker boxes can stack to 64 when dropped on the ground")
                                .extraInfo("To move them around between inventories, use shift click to move entire stacks").boolAccelerate().defaultFalse(),
  rule("explosionNoBlockDamage", "tnt", "Explosions won't destroy blocks"),
  rule("tntPrimerMomentumRemoved", "tnt", "Removes random TNT momentum when primed"),
  rule("fastRedstoneDust",      "experimental optimizations", "Lag optimizations for redstone dust")
                                .extraInfo("by Theosib").boolAccelerate().defaultFalse().wip(),
  rule("huskSpawningInTemples", "experimental feature", "Only husks spawn in desert temples").boolAccelerate().wip(),
  rule("shulkerSpawningInEndCities", "feature experimental", "Shulkers will respawn in end cities").boolAccelerate(),
  rule("unloadedEntityFix",     "experimental creative", "Entities pushed or moved into unloaded chunks no longer disappear").wip(),
  rule("TNTDoNotUpdate",        "tnt", "TNT doesn't update when placed against a power source"),
  rule("antiCheatSpeed",        "creative surival", "Prevents players from rubberbanding when moving too fast").wip(),
  rule("quasiConnectivity",     "creative", "Pistons, droppers and dispensers react if block above them is powered")
                                .defaultTrue().wip(),
  rule("flippinCactus",         "creative survival", "Players can flip and rotate blocks when holding cactus")
                                .extraInfo("Doesn't cause block updates when rotated/flipped",
                                           "Applies to pistons, observers, droppers, repeaters, stairs, glazed terracotta etc...").wip(),
  rule("hopperCounters",        "commands creative survival","hoppers pointing to wool will count items passing through them")
                                .extraInfo("Enables /counter command, and actions while placing red and green carpets on wool blocks",
                                           "Use /counter <color?> reset to reset the counter, and /counter <color?> to query",
                                           "In survival, place green carpet on same color wool to query, red to reset the counters",
                                           "Counters are global and shared between players, 16 channels available",
                                           "Items counted are destroyed, count up to one stack per tick per hopper")
                                .isACommand().boolAccelerate().defaultFalse(),
  rule("renewableSponges",      "experimental feature", "Guardians turn into Elder Guardian when struck by lightning"),
  rule("movableTileEntities",   "experimental", "Pistons can push tile entities, like hoppers, chests etc.").boolAccelerate(),
  rule("desertShrubs",          "feature", "Saplings turn into dead shrubs in hot climates and no water access when it attempts to grow into a tree").wip(),
  rule("silverFishDropGravel",  "experimental", "Silverfish drop a gravel item when breaking out of a block"),
  rule("summonNaturalLightning","creative", "summoning a lightning bolt has all the side effects of natural lightning"),
  rule("commandSpawn",          "commands", "Enables /spawn command for spawn tracking").isACommand(),
  rule("commandTick",           "commands", "Enables /tick command to control game speed").isACommand(),
  rule("commandLog",            "commands", "Enables /log command to monitor events in the game via chat and overlays").isACommand().wip(),
  rule("commandDistance",       "commands", "Enables /distance command to measure in game distance between points").isACommand()
                                .extraInfo("Also enables brown carpet placement action if 'carpets' rule is turned on as well"),
  rule("commandInfo",           "commands", "Enables /info command for blocks and entities").isACommand()
                                .extraInfo("Also enables gray carpet placement action ")
                                .extraInfo("and yellow carpet placement action for entities if 'carpets' rule is turned on as well"),
  rule("commandCameramode",     "commands", "Enables /c and /s commands to quickly switch between camera and survival modes").isACommand()
                                .extraInfo("/c and /s commands are available to all players regardless of their permission levels"),
  rule("commandPerimeterInfo",  "commands", "Enables /perimeterinfo command").isACommand()
                                .extraInfo("... that scans the area around the block for potential spawnable spots"),
  rule("commandDraw",  "commands", "Enables /draw commands").isACommand()
                                .extraInfo("... allows for drawing simple shapes"),
  rule("commandScript",  "commands", "Enables /script command").isACommand()
                                .extraInfo("a powerful in-game scripting API"),
  rule("commandPlayer",         "commands", "Enables /player command to control/spawn players").isACommand().wip(),
  rule("carpets",               "survival", "Placing carpets may issue carpet commands for non-op players"),
  rule("missingTools",          "survival", "Pistons, Glass and Sponge can be broken faster with their appropriate tools").wip(),
  rule("portalCaching",         "survival experimental", "Alternative, persistent caching strategy for nether portals").wip(),
  rule("calmNetherFires",       "experimental", "Permanent fires don't schedule random updates").wip(),
  rule("fillUpdates",           "creative", "fill/clone/setblock and structure blocks cause block updates").defaultTrue().boolAccelerate(),
  rule("pushLimit",             "creative","Customizable piston push limit")
                                .choices("12","10 12 14 100").setNotStrict().numAccelerate(),
  rule("railPowerLimit",        "creative", "Customizable powered rail power range")
                                .choices("9","9 15 30").setNotStrict().validate( (s, n) ->
                                    railPowerLimitAdjusted = CarpetSettings.getInt("railPowerLimit") - 1),
  rule("fillLimit",             "creative","Customizable fill/clone volume limit")
                                .choices("32768","32768 250000 1000000").setNotStrict(),
  rule("maxEntityCollisions",   "optimizations", "Customizable maximal entity collision limits, 0 for no limits")
                                .choices("0","0 1 20").setNotStrict().numAccelerate(),
  //???rule("pistonGhostBlocksFix",  "fix", "Fix for piston ghost blocks")
  //                              .extraInfo("true(serverOnly) option works with all clients, including vanilla",
  //                              "clientAndServer option requires compatible carpet clients and messes up flying machines")
  //                              .choices("false","false true clientAndServer"),
  //???rule("waterFlow",             "optimizations", "fixes water flowing issues")
  //                              .choices("vanilla","vanilla optimized correct"),
  rule("onePlayerSleeping",     "survival", "One player is required on the server to cause night to pass"),
  rule("customMOTD",            "creative","Sets a different motd message on client trying to connect to the server")
                                .extraInfo("use '_' to use the startup setting from server.properties")
                                .choices("_","_").setNotStrict().validate((s, n) ->
                                    ((MinecraftServer_motdInterface)CarpetServer.minecraft_server).checkMOTD()),
  rule("rotatorBlock",          "experimental", "Cactus in dispensers rotates blocks.")
                                .extraInfo("Cactus in a dispenser gives the dispenser the ability to rotate the blocks " +
                                           "that are in front of it anti-clockwise if possible.").wip(),
  rule("viewDistance",          "creative", "Changes the view distance of the server.")
                                .extraInfo("Set to 0 to not override the value in server settings.")
                                .choices("0", "0 12 16 32 64").setNotStrict()
                                .validate( (s, n) -> {
                                    int viewDistance = getInt("viewDistance");
                                    if (CarpetServer.minecraft_server.isDedicated())
                                    {
                                        if (viewDistance < 2)
                                        {
                                            viewDistance = ((DedicatedServer) CarpetServer.minecraft_server).getProperties().viewDistance;
                                        }
                                        if (viewDistance > 64)
                                        {
                                            viewDistance = 64;
                                        }
                                        if (viewDistance != CarpetServer.minecraft_server.getPlayerManager().getViewDistance())
                                            CarpetServer.minecraft_server.getPlayerManager().setViewDistance(viewDistance);
                                    }
                                    else
                                    {
                                        CarpetSettings.get(n).setForce("0");
                                        if (s != null)
                                            Messenger.m(s, "w view distance can only be changed on a server");
                                    }
                                }),
  rule("disableSpawnChunks",      "creative", "Removes the spawn chunks.")
                                  .validate((s, n) -> {
                                      if (!CarpetSettings.getBool("disableSpawnChunks"))
                                          if (s != null)
                                              Messenger.m(s, "w Spawn chunks re-enabled. Visit spawn to load it.");
                                  }).boolAccelerate().wip(),
  rule("kelpGenerationGrowLimit", "feature", "limits growth limit of newly naturally generated kelp to this amount of blocks")
                                  .choices("25", "0 2 25").setNotStrict(),
  rule("renewableCoral",          "feature", "Coral structures will grow with bonemeal from coral plants"),
  rule("placementRotationFix",    "fix", "fixes block placement rotation issue when player rotates quickly while placing blocks"),
  rule("leadFix",                 "fix", "Fixes leads breaking/becoming invisible in unloaded chunks")
                                  .extraInfo("You may still get visibly broken leash links on the client side, but server side the link is still there.").wip(),
  rule("lagFreeSpawning", "optimizations", "Spawning requires much less CPU and Memory").boolAccelerate()
        };
        for (CarpetSettingEntry rule: RuleList)
        {
            settings_store.put(rule.getName(), rule);
        }
    }

    public static void notifyPlayersCommandsChanged()
    {
        if (CarpetServer.minecraft_server == null || CarpetServer.minecraft_server.getPlayerManager() == null)
        {
            return;
        }
        for (ServerPlayerEntity entityplayermp : CarpetServer.minecraft_server.getPlayerManager().getPlayerList())
        {
            CarpetServer.minecraft_server.getCommandManager().sendCommandTree(entityplayermp);
        }
    }

    public static void apply_settings_from_conf(MinecraftServer server)
    {
        Map<String, String> conf = read_conf(server);
        boolean is_locked = locked;
        locked = false;
        if (is_locked)
        {
            LOG.info("[CM]: Carpet Mod is locked by the administrator");
        }
        for (String key: conf.keySet())
        {
            set(server.getCommandSource(), key, conf.get(key));
            LOG.info("[CM]: loaded setting "+key+" as "+conf.get(key)+" from carpet.conf");
        }
        locked = is_locked;
    }
    private static void disable_commands_by_default()
    {
        for (CarpetSettingEntry entry: settings_store.values())
        {
            if (entry.getName().startsWith("command"))
            {
                entry.defaultFalse();
            }
        }
    }

    private static Map<String, String> read_conf(MinecraftServer server)
    {
        try
        {
            File settings_file = server.getLevelStorage().resolveFile(server.getLevelName(), "carpet.conf");
            BufferedReader b = new BufferedReader(new FileReader(settings_file));
            String line = "";
            Map<String,String> result = new HashMap<String, String>();
            while ((line = b.readLine()) != null)
            {
                line = line.replaceAll("\\r|\\n", "");
                if ("locked".equalsIgnoreCase(line))
                {
                    disable_commands_by_default();
                    locked = true;
                }
                String[] fields = line.split("\\s+",2);
                if (fields.length > 1)
                {
                    if (get(fields[0])==FalseEntry)
                    {
                        LOG.error("[CM]: Setting " + fields[0] + " is not a valid - ignoring...");
                        continue;
                    }
                    if (!(Arrays.asList(get(fields[0]).getOptions()).contains(fields[1])) && get(fields[0]).isStrict())
                    {
                        LOG.error("[CM]: The value of " + fields[1] + " for " + fields[0] + " is not valid - ignoring...");
                        continue;
                    }
                    result.put(fields[0],fields[1]);
                }
            }
            b.close();
            return result;
        }
        catch(FileNotFoundException e)
        {
            return new HashMap<>();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return new HashMap<>();
        }
        
    }
    private static void write_conf(MinecraftServer server, Map<String, String> values)
    {
        if (locked) return;
        try
        {
            File settings_file = server.getLevelStorage().resolveFile(server.getLevelName(), "carpet.conf");
            FileWriter fw = new FileWriter(settings_file);
            for (String key: values.keySet())
            {
                fw.write(key+" "+values.get(key)+"\n");
            }
            fw.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            LOG.error("[CM]: failed write the carpet.conf");
        }
    }
    
    // stores different defaults in the file
    public static boolean setDefaultRule(ServerCommandSource source, String setting_name, String string_value)
    {
        if (locked) return false;
        if (settings_store.containsKey(setting_name))
        {
            Map<String, String> conf = read_conf(source.getMinecraftServer());
            conf.put(setting_name, string_value);
            write_conf(source.getMinecraftServer(), conf);
            set(source, setting_name,string_value);
            return true;
        }
        return false;
    }
    // removes overrides of the default values in the file  
    public static boolean removeDefaultRule(ServerCommandSource source, String setting_name)
    {
        if (locked) return false;
        if (settings_store.containsKey(setting_name))
        {
            Map<String, String> conf = read_conf(source.getMinecraftServer());
            conf.remove(setting_name);
            write_conf(source.getMinecraftServer(), conf);
            set(source, setting_name,get(setting_name).getDefault());
            return true;
        }
        return false;
    }

    //changes setting temporarily
    public static boolean set(ServerCommandSource source, String setting_name, String string_value)
    {
        CarpetSettingEntry en = get(setting_name);
        if (en != FalseEntry)
        {
            en.set(source, string_value);
            //reload_stat(setting_name);
            //CarpetClientRuleChanger.updateCarpetClientsRule(setting_name, string_value);
            return true;
        }
        return false;
    }

    // used as CarpetSettings.get("pushLimit").integer to get the int value of push limit
    public static CarpetSettingEntry get(String setting_name)
    {
        if (!settings_store.containsKey(setting_name) )
        {
            return FalseEntry;
        }
        return settings_store.get(setting_name);
    } 
    
    public static int getInt(String setting_name)
    {
        return get(setting_name).getIntegerValue();
    }
    public static boolean getBool(String setting_name)
    {
        return get(setting_name).getBoolValue();
    }
    public static String getString(String setting_name) { return get(setting_name).getStringValue(); }
    public static float getFloat(String setting_name)
    {
        return get(setting_name).getFloatValue();
    }
    public static CarpetSettingEntry[] findAll(String tag)
    {
        ArrayList<CarpetSettingEntry> res = new ArrayList<CarpetSettingEntry>();
        for (String rule: settings_store.keySet().stream().sorted().collect(Collectors.toList()))
        {
            if (tag == null || settings_store.get(rule).matches(tag))
            {
                res.add(settings_store.get(rule));
            }
        }
        return res.toArray(new CarpetSettingEntry[0]);
    }
    public static CarpetSettingEntry[] find_nondefault(MinecraftServer server)
    {
        ArrayList<CarpetSettingEntry> res = new ArrayList<CarpetSettingEntry>();
        Map <String,String> defaults = read_conf(server);
        for (String rule: settings_store.keySet().stream().sorted().collect(Collectors.toList()))
        {
            if (!settings_store.get(rule).isDefault() || defaults.containsKey(rule))
            {
                res.add(settings_store.get(rule));
            }
        }
        return res.toArray(new CarpetSettingEntry[0]);
    }
    public static CarpetSettingEntry[] findStartupOverrides(MinecraftServer server)
    {
        ArrayList<CarpetSettingEntry> res = new ArrayList<CarpetSettingEntry>();
        if (locked) return res.toArray(new CarpetSettingEntry[0]);
        Map <String,String> defaults = read_conf(server);
        for (String rule: settings_store.keySet().stream().sorted().collect(Collectors.toList()))
        {
            if (defaults.containsKey(rule))
            {
                res.add(settings_store.get(rule));
            }
        }
        return res.toArray(new CarpetSettingEntry[0]);
    }
    public static String[] toStringArray(CarpetSettingEntry[] entry_array)
    {
        return Stream.of(entry_array).map(CarpetSettingEntry::getName).toArray( String[]::new );
    }
    public static ArrayList<CarpetSettingEntry> getAllCarpetSettings()
    {
        ArrayList<CarpetSettingEntry> res = new ArrayList<CarpetSettingEntry>();
        for (String rule: settings_store.keySet().stream().sorted().collect(Collectors.toList()))
        {
            res.add(settings_store.get(rule));
        }
        
        return res;
    }
    public static CarpetSettingEntry getCarpetSetting(String rule)
    {
        return settings_store.get(rule);
    }
    
    public static void resetToVanilla(ServerCommandSource source)
    {
        for (String rule: settings_store.keySet())
        {
            get(rule).reset(source);
            //reload_stat(rule);
        }
    }
    
    public static void resetToUserDefaults(ServerCommandSource source)
    {
        resetToVanilla(source);
        apply_settings_from_conf(source.getMinecraftServer());
    }
    
    public static void resetToCreative(ServerCommandSource source)
    {
        resetToBugFixes(source);
        set(source, "fillLimit","500000");
        set(source, "fillUpdates","false");
        set(source, "portalCreativeDelay","true");
        set(source, "portalCaching","true");
        set(source, "flippinCactus","true");
        set(source, "hopperCounters","true");
        set(source, "antiCheatSpeed","true");
        
    }
    public static void resetToSurvival(ServerCommandSource source)
    {
        resetToBugFixes(source);
        set(source, "ctrlQCraftingFix","true");
        set(source, "persistentParrots", "true");
        set(source, "stackableEmptyShulkerBoxes","true");
        set(source, "flippinCactus","true");
        set(source, "hopperCounters","true");
        set(source, "carpets","true");
        set(source, "missingTools","true");
        set(source, "portalCaching","true");
        set(source, "miningGhostBlocksFix","true");
    }
    public static void resetToBugFixes(ServerCommandSource source)
    {
        resetToVanilla(source);
        set(source, "portalSuffocationFix","true");
        set(source, "pistonGhostBlocksFix","serverOnly");
        set(source, "portalTeleportationFix","true");
        set(source, "entityDuplicationFix","true");
        set(source, "inconsistentRedstoneTorchesFix","true");
        set(source, "llamaOverfeedingFix","true");
        set(source, "invisibilityFix","true");
        set(source, "potionsDespawnFix","true");
        set(source, "liquidsNotRandom","true");
        set(source, "mobsDontControlMinecarts","true");
        set(source, "breedingMountingDisabled","true");
        set(source, "growingUpWallJump","true");
        set(source, "reloadSuffocationFix","true");
        set(source, "watchdogFix","true");
        set(source, "unloadedEntityFix","true");
        set(source, "hopperDuplicationFix","true");
        set(source, "calmNetherFires","true");
    }

    public static class CarpetSettingEntry 
    {
        private String rule;
        private String string;
        private int integer;
        private boolean bool;
        private float flt;
        private String[] options;
        private String[] tags;
        private String toast;
        private String[] extra_info;
        private String default_string_value;
        private boolean isFloat;
        private boolean strict;
        private List<BiConsumer<ServerCommandSource, String>> validators;

        //factory
        public static CarpetSettingEntry create(String rule_name, String tags, String toast)
        {
            return new CarpetSettingEntry(rule_name, tags, toast);
        }
        private CarpetSettingEntry(String rule_name, String tags_string, String toast_string)
        {
            set(null, "false");
            rule = rule_name;
            default_string_value = string;
            tags = tags_string.split("\\s+"); // never empty
            toast = toast_string;
            options = "true false".split("\\s+");
            isFloat = false;
            extra_info = null;
            strict = true;
            validators = null;
        }
        public CarpetSettingEntry defaultTrue()
        {
            set(null, "true");
            default_string_value = string;
            options = "true false".split("\\s+");
            return this;
        }
        public CarpetSettingEntry validate(BiConsumer<ServerCommandSource, String> method)
        {
            if (validators == null)
            {
                validators = new ArrayList<>();
            }
            validators.add(method);
            return this;
        }
        public CarpetSettingEntry boolAccelerate()
        {
            BiConsumer<ServerCommandSource, String> validator = (source, name) -> {
                try
                {
                    Field f = CarpetSettings.class.getDeclaredField("b_"+name);
                    f.setBoolean(null, CarpetSettings.getBool(name));
                }
                catch (IllegalAccessException e)
                {
                    CarpetSettings.LOG.error("[CM Error] rule "+name+" has wrong access to boolean accelerator");
                }
                catch (NoSuchFieldException e)
                {
                    CarpetSettings.LOG.error("[CM Error] rule "+name+" doesn't have a boolean accelerator");
                }
            };
            return validate(validator);
        }
        public CarpetSettingEntry numAccelerate()
        {
            BiConsumer<ServerCommandSource, String> validator = (source, name) -> {
                try
                {
                    Field f = CarpetSettings.class.getDeclaredField("n_"+name);
                    if (CarpetSettings.get(name).isFloat)
                    {
                        f.setDouble(null, (double) CarpetSettings.getFloat(name));
                    }
                    else
                    {
                        f.setInt(null, CarpetSettings.getInt(name));
                    }
                }
                catch (IllegalAccessException e)
                {
                    CarpetSettings.LOG.error("[CM Error] rule "+name+" wrong type of numerical accelerator");
                }
                catch (NoSuchFieldException e)
                {
                    CarpetSettings.LOG.error("[CM Error] rule "+name+" doesn't have a numerical accelerator");
                }
            };
            return validate(validator);
        }


        public CarpetSettingEntry isACommand()
        {
            return this.defaultTrue().validate( (s, n) -> notifyPlayersCommandsChanged());
        }

        public CarpetSettingEntry defaultFalse()
        {
            set(null, "false");
            default_string_value = string;
            options = "true false".split("\\s+");
            return this;
        }

        public CarpetSettingEntry choices(String defaults, String options_string)
        {
            set(null, defaults);
            default_string_value = string;
            options =  options_string.split("\\s+");
            return this;
        }
        public CarpetSettingEntry extraInfo(String ... extra_info_string)
        {
            extra_info = extra_info_string;
            return this;
        }

        public CarpetSettingEntry setFloat()
        {
            isFloat = true;
            strict = false;
            return this;
        }

        public CarpetSettingEntry setNotStrict()
        {
            strict = false;
            return this;
        }

        private void set(ServerCommandSource source, String unparsed)
        {
            setForce(unparsed);
            if (validators != null)
            {
                validators.forEach((r) -> r.accept(source, this.getName()));
            }
        }
        private void setForce(String unparsed)
        {
            string = unparsed;
            try
            {
                integer = Integer.parseInt(unparsed);
            }
            catch(NumberFormatException e)
            {
                integer = 0;
            }
            try
            {
                flt = Float.parseFloat(unparsed);
            }
            catch(NumberFormatException e)
            {
                flt = 0.0F;
            }
            bool = (integer > 0) || Boolean.parseBoolean(unparsed);
        }

        //accessors
        public boolean isDefault() { return string.equals(default_string_value); }
        public String getDefault() { return default_string_value; }
        public String toString() { return rule + ": " + string; }
        public String getToast() { return toast; }
        public String[] getInfo() { return extra_info == null?new String[0]:extra_info; }
        public String[] getOptions() { return options;}
        public String[] getTags() { return tags; }
        public String getName() { return rule; }
        public String getStringValue() { return string; }
        public boolean getBoolValue() { return bool; }
        public int getIntegerValue() { return integer; }
        public float getFloatValue() { return flt; }
        public boolean getIsFloat() { return isFloat;}

        //actual stuff
        public void reset(ServerCommandSource s)
        {
            set(s, default_string_value);
        }

        public boolean matches(String tag)
        {
            tag = tag.toLowerCase();
            if (rule.toLowerCase().contains(tag))
            {
                return true;
            }
            for (String t: tags)
            {
                if (tag.equalsIgnoreCase(t))
                {
                    return true;
                }
            }
            return false;
        }
        public String getNextValue()
        {
            int i;
            for(i = 0; i < options.length; i++)
            {
                if(options[i].equals(string))
                {
                    break;
                }
            }
            i++;
            return options[i % options.length];
        }

        public boolean isStrict()
        {
            return strict;
        }

        public CarpetSettingEntry wip()
        {
            return validate( (s, n) -> {
               if (s != null)
                   Messenger.m(s, "r Work in progress - limited or no functionality available for "+n);
            });
        }
    }
}

