package carpet.logging;

import net.minecraft.network.chat.BaseComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TypeLogger<T> extends Logger
{

    public final Map<String, T> parsedPlayerOptions;
    public final Class<T> type;


    static <T> Logger stardardTypeLogger(String logName, String def, Class<T> type, String[] options)
    {
        return stardardTypeLogger(logName, def, type, options, false);
    }

    static <T> Logger stardardTypeLogger(String logName, String def, Class<T> type, String[] options, boolean strictOptions)
    {
        // should convert to factory method if more than 2 classes are here
        try
        {
            return new TypeLogger<>(LoggerRegistry.class.getField("__"+logName), logName, def, type, options, strictOptions);
        }
        catch (NoSuchFieldException e)
        {
            throw new RuntimeException("Failed to create logger "+logName);
        }
    }

    @Deprecated
    public TypeLogger(Field field, String logName, String def, Class<T> type, String[] options) {
        this(field, logName, def, type, options, false);
    }

    public TypeLogger(Field field, String logName, String def, Class<T> type, String[] options, boolean strictOptions)
    {
        super(field, logName, def, options, strictOptions);
        this.parsedPlayerOptions = new HashMap<>();
        this.type = type;
    }

    @Override
    public void addPlayer(String playerName, String option)
    {
        T number = parse(option); // Throws an error that LogCommand will catch if invalid
        if (number == null) {
            throw new NullPointerException("Value was not seen as valid: "+option);
        }
        parsedPlayerOptions.put(playerName, number);
        super.addPlayer(playerName,option);
    }

    @Override
    public void removePlayer(String playerName)
    {
        parsedPlayerOptions.remove(playerName);
        super.removePlayer(playerName);
    }

    /**
     * serves messages to players fetching them from the promise
     * will repeat invocation for players that share the same option
     */
    @FunctionalInterface
    public interface typeMessage<T> { BaseComponent [] get(T playerOption, Player player);}
    public void log(typeMessage<T> messagePromise)
    {
        for (Map.Entry<String,T> en : parsedPlayerOptions.entrySet())
        {
            ServerPlayer player = playerFromName(en.getKey());
            if (player != null)
            {
                BaseComponent [] messages = messagePromise.get(en.getValue(),player);
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
    public interface typeMessageIgnorePlayer<T> { BaseComponent [] get(T playerOption);}
    public void log(typeMessageIgnorePlayer<T> messagePromise)
    {
        Map<T, BaseComponent[]> cannedMessages = new HashMap<>();
        for (Map.Entry<String,T> en : parsedPlayerOptions.entrySet())
        {
            ServerPlayer player = playerFromName(en.getKey());
            if (player != null)
            {
                T option = en.getValue();
                if (!cannedMessages.containsKey(option))
                {
                    cannedMessages.put(option,messagePromise.get(option));
                }
                BaseComponent [] messages = cannedMessages.get(option);
                if (messages != null)
                    sendPlayerMessage(player, messages);
            }
        }
    }

    public T parse(String value)
    {
        if (type == String.class)
        {
            return (T) value;
        }
        else if (type == boolean.class)
        {
            return (T) (Object) Boolean.parseBoolean(value);
        }
        else if (type == int.class)
        {
            return (T) (Object) Integer.parseInt(value);
        }
        else if (type == double.class)
        {
            return (T) (Object) Double.parseDouble(value);
        }
        else if (type.isEnum())
        {
            String ucValue = value.toUpperCase(Locale.ROOT);
            return (T) Enum.valueOf((Class<? extends Enum>) type, ucValue);
        }
        else
        {
            return null;
        }
    }
}
