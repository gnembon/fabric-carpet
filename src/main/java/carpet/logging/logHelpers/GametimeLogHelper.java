package carpet.logging.logHelpers;

import carpet.CarpetServer;
import carpet.logging.Logger;
import carpet.logging.LoggerRegistry;
import carpet.utils.Messenger;
import com.google.common.collect.Sets;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.BaseText;

import java.util.Set;

public class GametimeLogHelper
{
    private static Set<String> playersLogged = Sets.newHashSet();

    public static void onLoggerLogged(Logger logger)
    {
        playersLogged.addAll(logger.getSubscribedOnlinePlayers().keySet());
    }

    public static void onTimeTicked(long gametime)
    {
        playersLogged.forEach((name) -> {
            ServerPlayerEntity playerEntity = CarpetServer.minecraft_server.getPlayerManager().getPlayer(name);
            BaseText message = Messenger.c("rb ==== tick " + gametime + " ended ====");
            if (playerEntity == null) { return; }
            LoggerRegistry.getLogger("gametime").sendPlayerMessage(playerEntity, message);
        });
        playersLogged.clear();
    }
}
