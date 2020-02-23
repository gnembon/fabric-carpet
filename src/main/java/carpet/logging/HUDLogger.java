package carpet.logging;

import carpet.utils.HUDController;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.BaseText;

import java.lang.reflect.Field;

public class HUDLogger extends Logger
{
    static Logger stardardHUDLogger(String logName, String def, String [] options)
    {
        // should convert to factory method if more than 2 classes are here
        try
        {
            return new HUDLogger(LoggerRegistry.class.getField("__"+logName), logName, def, options);
        }
        catch (NoSuchFieldException e)
        {
            throw new RuntimeException("Failed to create logger "+logName);
        }
    }

    public HUDLogger(Field field, String logName, String def, String[] options)
    {
        super(field, logName, def, options);
    }

    @Override
    public void removePlayer(String playerName)
    {
        ServerPlayerEntity player = playerFromName(playerName);
        if (player != null) HUDController.clear_player(player);
        super.removePlayer(playerName);
    }

    @Override
    public void sendPlayerMessage(ServerPlayerEntity player, BaseText... messages)
    {
        for (BaseText m:messages) HUDController.addMessage(player, m);
    }


}
