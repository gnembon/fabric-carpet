package carpet.logging;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class Logger
{
    // The set of subscribed and online players.
    private Map<String, String> subscribedOnlinePlayers;

    // The set of subscribed and offline players.
    private Map<String,String> subscribedOfflinePlayers;

    // The logName of this log. Gets prepended to logged messages.
    private String logName;

    private String default_option;

    private String[] options;

    private Field acceleratorField;

    private boolean strictOptions;

    static Logger stardardLogger(String logName, String def, String [] options)
    {
        return stardardLogger(logName, def, options, false);
    }

    static Logger stardardLogger(String logName, String def, String [] options, boolean strictOptions)
    {
        try
        {
            return new Logger(LoggerRegistry.class.getField("__"+logName), logName, def, options, strictOptions);
        }
        catch (NoSuchFieldException e)
        {
            throw new RuntimeException("Failed to create logger "+logName);
        }
    }

    @Deprecated
    public Logger(Field acceleratorField, String logName, String def, String [] options) {
        this(acceleratorField, logName, def, options, false);
    }

    public Logger(Field acceleratorField, String logName, String def, String [] options, boolean strictOptions)
    {
        subscribedOnlinePlayers = new HashMap<>();
        subscribedOfflinePlayers = new HashMap<>();
        this.acceleratorField = acceleratorField;
        this.logName = logName;
        this.default_option = def;
        this.options = options == null ? new String[0] : options;
        this.strictOptions = strictOptions;
        if (acceleratorField == null)
            CarpetSettings.LOG.error("[CM] Logger "+getLogName()+" is missing a specified accelerator");
    }

    public String getDefault()
    {
        return default_option;
    }
    public String [] getOptions()
    {
        return options;
    }
    public String getLogName()
    {
        return logName;
    }

    /**
     * Subscribes the player with the given logName to the logger.
     */
    public void addPlayer(String playerName, String option)
    {
        if (playerFromName(playerName) != null)
        {
            subscribedOnlinePlayers.put(playerName, option);
        }
        else
        {
            subscribedOfflinePlayers.put(playerName, option);
        }
        LoggerRegistry.setAccess(this);
    }

    /**
     * Unsubscribes the player with the given logName from the logger.
     */
    public void removePlayer(String playerName)
    {
        subscribedOnlinePlayers.remove(playerName);
        subscribedOfflinePlayers.remove(playerName);
        LoggerRegistry.setAccess(this);
    }

    /**
     * Returns true if there are any online subscribers for this log.
     */
    public boolean hasOnlineSubscribers()
    {
        return subscribedOnlinePlayers.size() > 0;
    }

    public void serverStopped()
    {
        subscribedOnlinePlayers.clear();
        subscribedOfflinePlayers.clear();
    }

    public Field getField()
    {
        return acceleratorField;
    }

    /**
     * serves messages to players fetching them from the promise
     * will repeat invocation for players that share the same option
     */
    @FunctionalInterface
    public interface lMessage { Component [] get(String playerOption, Player player);}
    public void log(lMessage messagePromise)
    {
        for (Map.Entry<String,String> en : subscribedOnlinePlayers.entrySet())
        {
            ServerPlayer player = playerFromName(en.getKey());
            if (player != null)
            {
                Component [] messages = messagePromise.get(en.getValue(),player);
                if (messages != null)
                    sendPlayerMessage(player, messages);
            }
        }
    }

    /**
     * guarantees that each message for each option will be evaluated once from the promise
     * and served the same way to all other players subscribed to the same option
     */
    @FunctionalInterface
    public interface lMessageIgnorePlayer { Component [] get(String playerOption);}
    public void log(lMessageIgnorePlayer messagePromise)
    {
        Map<String, Component[]> cannedMessages = new HashMap<>();
        for (Map.Entry<String,String> en : subscribedOnlinePlayers.entrySet())
        {
            ServerPlayer player = playerFromName(en.getKey());
            if (player != null)
            {
                String option = en.getValue();
                if (!cannedMessages.containsKey(option))
                {
                    cannedMessages.put(option,messagePromise.get(option));
                }
                Component [] messages = cannedMessages.get(option);
                if (messages != null)
                    sendPlayerMessage(player, messages);
            }
        }
    }
    /**
     * guarantees that message is evaluated once, so independent from the player and chosen option
     */
    public void log(Supplier<Component[]> messagePromise)
    {
        Component [] cannedMessages = null;
        for (Map.Entry<String,String> en : subscribedOnlinePlayers.entrySet())
        {
            ServerPlayer player = playerFromName(en.getKey());
            if (player != null)
            {
                if (cannedMessages == null) cannedMessages = messagePromise.get();
                sendPlayerMessage(player, cannedMessages);
            }
        }
    }

    public void sendPlayerMessage(ServerPlayer player, Component ... messages)
    {
        Arrays.stream(messages).forEach(player::sendSystemMessage);
    }

    /**
     * Gets the {@code PlayerEntity} instance for a player given their UUID. Returns null if they are offline.
     */
    protected ServerPlayer playerFromName(String name)
    {
        return CarpetServer.minecraft_server.getPlayerList().getPlayerByName(name);
    }

    // ----- Event Handlers ----- //

    public void onPlayerConnect(Player player, boolean firstTime)
    {
        // If the player was subscribed to the log and offline, move them to the set of online subscribers.
        String playerName = player.getName().getString();
        if (subscribedOfflinePlayers.containsKey(playerName))
        {
            subscribedOnlinePlayers.put(playerName, subscribedOfflinePlayers.get(playerName));
            subscribedOfflinePlayers.remove(playerName);
        }
        else if(firstTime)
        {
            Set<String> loggingOptions = new HashSet<>(Arrays.asList(CarpetSettings.defaultLoggers.split(",")));
            String logName = getLogName();
            for (String str : loggingOptions) {
                String[] vars = str.split(" ", 2);
                if (vars[0].equals(logName)) {
                    LoggerRegistry.subscribePlayer(playerName, getLogName(), vars.length == 1 ? getDefault() : vars[1]);
                    break;
                }
            }
        }
        LoggerRegistry.setAccess(this);
    }

    public void onPlayerDisconnect(Player player)
    {
        // If the player was subscribed to the log, move them to the set of offline subscribers.
        String playerName = player.getName().getString();
        if (subscribedOnlinePlayers.containsKey(playerName))
        {
            subscribedOfflinePlayers.put(playerName, subscribedOnlinePlayers.get(playerName));
            subscribedOnlinePlayers.remove(playerName);
        }
        LoggerRegistry.setAccess(this);
    }

    public String getAcceptedOption(String arg)
    {
        if (Arrays.asList(this.getOptions()).contains(arg)) return arg;
        return null;
    }

    public boolean isOptionValid(String option) {
        if (strictOptions)
        {
            return Arrays.asList(this.getOptions()).contains(option);
        }
        return option != null;
    }
}
