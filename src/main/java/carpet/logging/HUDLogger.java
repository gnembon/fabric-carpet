package carpet.logging;

import carpet.utils.HUDController;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.BaseText;

public class HUDLogger extends Logger
{
    public HUDLogger(String logName, String def, String[] options)
    {
        super(logName, def, options);
    }

    @Override
    public void removePlayer(String playerName)
    {
        PlayerEntity player = playerFromName(playerName);
        if (player != null) HUDController.clear_player(player);
        super.removePlayer(playerName);
    }

    @Override
    public void sendPlayerMessage(PlayerEntity player, BaseText... messages)
    {
        for (BaseText m:messages) HUDController.addMessage(player, m);
    }


}
