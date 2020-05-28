package carpet.network;

import carpet.CarpetServer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Identifier;

public class CarpetClient
{
    public static final Object sync = new Object();
    public static final int HI = 69;
    public static final int HELLO = 420;
    public static final int DATA = 1;

    private static ClientPlayerEntity clientPlayer = null;
    private static boolean isServerCarpet = false;
    public static String serverCarpetVersion;
    public static final Identifier CARPET_CHANNEL = new Identifier("carpet:hello");

    public static void gameJoined(ClientPlayerEntity player)
    {
        synchronized (sync)
        {
            clientPlayer = player;
            // client didn't say hi back yet
            if (isServerCarpet)
                ClientNetworkHandler.respondHello();

        }
    }

    public static void disconnect()
    {
        isServerCarpet = false;
        clientPlayer = null;
        CarpetServer.disconnect();
    }

    public static void setCarpet()
    {
        isServerCarpet = true;
    }

    public static ClientPlayerEntity getPlayer()
    {
        return clientPlayer;
    }

    public static boolean isCarpet()
    {
        return isServerCarpet;
    }
}
