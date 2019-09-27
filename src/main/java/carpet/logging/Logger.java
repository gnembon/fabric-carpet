package carpet.logging;

import carpet.CarpetServer;
import carpet.settings.CarpetSettings;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.BaseText;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

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

    public Logger(String logName, String def, String [] options)
    {
        subscribedOnlinePlayers = new HashMap<>();
        subscribedOfflinePlayers = new HashMap<>();
        this.logName = logName;
        this.default_option = def;
        this.options = options;
    }

    public String getDefault()
    {
        return default_option;
    }
    public String [] getOptions()
    {
        if (options == null)
        {
            return new String[0];
        }
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

    /**
     * serves messages to players fetching them from the promise
     * will repeat invocation for players that share the same option
     */
    @FunctionalInterface
    public interface lMessage { BaseText [] get(String playerOption, PlayerEntity player);}
    public void log(lMessage messagePromise)
    {
        for (Map.Entry<String,String> en : subscribedOnlinePlayers.entrySet())
        {
            ServerPlayerEntity player = playerFromName(en.getKey());
            if (player != null)
            {
                BaseText [] messages = messagePromise.get(en.getValue(),player);
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
    public interface lMessageIgnorePlayer { BaseText [] get(String playerOption);}
    public void log(lMessageIgnorePlayer messagePromise)
    {
        Map<String, BaseText[]> cannedMessages = new HashMap<>();
        for (Map.Entry<String,String> en : subscribedOnlinePlayers.entrySet())
        {
            ServerPlayerEntity player = playerFromName(en.getKey());
            if (player != null)
            {
                String option = en.getValue();
                if (!cannedMessages.containsKey(option))
                {
                    cannedMessages.put(option,messagePromise.get(option));
                }
                BaseText [] messages = cannedMessages.get(option);
                if (messages != null)
                    sendPlayerMessage(player, messages);
            }
        }
    }
    /**
     * guarantees that message is evaluated once, so independent from the player and chosen option
     */
    public void log(Supplier<BaseText[]> messagePromise)
    {
        BaseText [] cannedMessages = null;
        for (Map.Entry<String,String> en : subscribedOnlinePlayers.entrySet())
        {
            ServerPlayerEntity player = playerFromName(en.getKey());
            if (player != null)
            {
                if (cannedMessages == null) cannedMessages = messagePromise.get();
                sendPlayerMessage(player, cannedMessages);
            }
        }
    }

    public void sendPlayerMessage(ServerPlayerEntity player, BaseText ... messages)
    {
        Arrays.stream(messages).forEach(player::sendMessage);
    }

    /**
     * Gets the {@code PlayerEntity} instance for a player given their UUID. Returns null if they are offline.
     */
    protected ServerPlayerEntity playerFromName(String name)
    {
        return CarpetServer.minecraft_server.getPlayerManager().getPlayer(name);
    }

    // ----- Event Handlers ----- //

    public void onPlayerConnect(PlayerEntity player, boolean firstTime)
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
            if (loggingOptions.contains(getLogName()))
            {
                subscribedOnlinePlayers.put(playerName, getDefault());
            }
        }
        LoggerRegistry.setAccess(this);
    }

    public void onPlayerDisconnect(PlayerEntity player)
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
        if (options != null && Arrays.asList(options).contains(arg)) return arg;
        return null;
    }
}
