package carpet.logging;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;

public class LoggerRegistry
{
    // Map from logger names to loggers.
    private static final Map<String, Logger> loggerRegistry = new HashMap<>();
    // Map from player names to the set of names of the logs that player is subscribed to.
    private static final Map<String, Map<String, String>> playerSubscriptions = new HashMap<>();
    //statics to quickly asses if its worth even to call each one
    public static boolean __tnt;
    public static boolean __projectiles;
    public static boolean __fallingBlocks;
    public static boolean __tps;
    public static boolean __counter;
    public static boolean __mobcaps;
    public static boolean __packets;
    public static boolean __pathfinding;
    public static boolean __explosions;

    public static void initLoggers()
    {
        stopLoggers();
        registerLoggers();
        CarpetServer.registerExtensionLoggers();
    }

    public static void registerLoggers()
    {
        registerLogger("tnt", Logger.stardardLogger( "tnt", "brief", new String[]{"brief", "full"}, true));
        registerLogger("projectiles", Logger.stardardLogger("projectiles", "brief",  new String[]{"brief", "full"}));
        registerLogger("fallingBlocks",Logger.stardardLogger("fallingBlocks", "brief", new String[]{"brief", "full"}));
        registerLogger("pathfinding", Logger.stardardLogger("pathfinding", "20", new String[]{"2", "5", "10"}));
        registerLogger("tps", HUDLogger.stardardHUDLogger("tps", null, null));
        registerLogger("packets", HUDLogger.stardardHUDLogger("packets", null, null));
        registerLogger("counter",HUDLogger.stardardHUDLogger("counter","white", Arrays.stream(DyeColor.values()).map(Object::toString).toArray(String[]::new)));
        registerLogger("mobcaps", HUDLogger.stardardHUDLogger("mobcaps", "dynamic",new String[]{"dynamic", "overworld", "nether","end"}));
        registerLogger("explosions", Logger.stardardLogger("explosions", "brief",new String[]{"brief", "full"}, true));

    }

    /**
     * Gets the logger with the given name. Returns null if no such logger exists.
     */
    public static Logger getLogger(String name) { return loggerRegistry.get(name); }

    /**
     * Gets the set of logger names.
     */
    public static Set<String> getLoggerNames() { return loggerRegistry.keySet(); }

    /**
     * Subscribes the player with name playerName to the log with name logName.
     */
    public static void subscribePlayer(String playerName, String logName, String option)
    {
        if (!playerSubscriptions.containsKey(playerName)) playerSubscriptions.put(playerName, new HashMap<>());
        Logger log = loggerRegistry.get(logName);
        if (option == null) option = log.getDefault();
        playerSubscriptions.get(playerName).put(logName,option);
        log.addPlayer(playerName, option);
    }

    /**
     * Unsubscribes the player with name playerName from the log with name logName.
     */
    public static void unsubscribePlayer(String playerName, String logName)
    {
        if (playerSubscriptions.containsKey(playerName))
        {
            Map<String,String> subscriptions = playerSubscriptions.get(playerName);
            subscriptions.remove(logName);
            loggerRegistry.get(logName).removePlayer(playerName);
            if (subscriptions.size() == 0) playerSubscriptions.remove(playerName);
        }
    }

    /**
     * If the player is not subscribed to the log, then subscribe them. Otherwise, unsubscribe them.
     */
    public static boolean togglePlayerSubscription(String playerName, String logName)
    {
        if (playerSubscriptions.containsKey(playerName) && playerSubscriptions.get(playerName).containsKey(logName))
        {
            unsubscribePlayer(playerName, logName);
            return false;
        }
        else
        {
            subscribePlayer(playerName, logName, null);
            return true;
        }
    }

    /**
     * Get the set of logs the current player is subscribed to.
     */
    public static Map<String,String> getPlayerSubscriptions(String playerName)
    {
        if (playerSubscriptions.containsKey(playerName))
        {
            return playerSubscriptions.get(playerName);
        }
        return null;
    }

    protected static void setAccess(Logger logger)
    {
        boolean value = logger.hasOnlineSubscribers();
        try
        {
            Field f = logger.getField();
            f.setBoolean(null, value);
        }
        catch (IllegalAccessException e)
        {
            CarpetSettings.LOG.error("Cannot change logger quick access field");
        }
    }
    /**
     * Called when the server starts. Creates the logs used by Carpet mod.
     */
    public static void registerLogger(String name, Logger logger)
    {
        loggerRegistry.put(name, logger);
        setAccess(logger);
    }

    private final static Set<String> seenPlayers = new HashSet<>();

    public static void stopLoggers()
    {
        for(Logger log: loggerRegistry.values() )
        {
            log.serverStopped();
        }
        seenPlayers.clear();
        loggerRegistry.clear();
        playerSubscriptions.clear();
    }
    public static void playerConnected(Player player)
    {
        boolean firstTime = false;
        if (!seenPlayers.contains(player.getName().getString()))
        {
            seenPlayers.add(player.getName().getString());
            firstTime = true;
            //subscribe them to the defualt loggers
        }
        for(Logger log: loggerRegistry.values() )
        {
            log.onPlayerConnect(player, firstTime);
        }
    }

    public static void playerDisconnected(Player player)
    {
        for(Logger log: loggerRegistry.values() )
        {
            log.onPlayerDisconnect(player);
        }
    }
}
