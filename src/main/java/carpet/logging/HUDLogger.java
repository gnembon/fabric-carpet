package carpet.logging;

import java.lang.reflect.Field;
import net.minecraft.network.chat.BaseComponent;
import net.minecraft.server.level.ServerPlayer;

public class HUDLogger extends Logger
{
    static Logger stardardHUDLogger(String logName, String def, String [] options)
    {
        return stardardHUDLogger(logName, def, options, false);
    }

    static Logger stardardHUDLogger(String logName, String def, String [] options, boolean strictOptions)
    {
        // should convert to factory method if more than 2 classes are here
        try
        {
            return new HUDLogger(LoggerRegistry.class.getField("__"+logName), logName, def, options, strictOptions);
        }
        catch (NoSuchFieldException e)
        {
            throw new RuntimeException("Failed to create logger "+logName);
        }
    }

    public HUDLogger(Field field, String logName, String def, String[] options, boolean strictOptions)
    {
        super(field, logName, def, options, strictOptions);
    }

    @Deprecated
    public HUDLogger(Field field, String logName, String def, String[] options) {
        super(field, logName, def, options, false);
    }

    @Override
    public void removePlayer(String playerName)
    {
        ServerPlayer player = playerFromName(playerName);
        if (player != null) HUDController.clear_player(player);
        super.removePlayer(playerName);
    }

    @Override
    public void sendPlayerMessage(ServerPlayer player, BaseComponent... messages)
    {
        for (BaseComponent m:messages) HUDController.addMessage(player, m);
    }


}
