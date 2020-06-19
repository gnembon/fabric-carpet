package carpet.network;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.script.utils.ShapesRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class CarpetClient
{
    public static final Object sync = new Object();
    public static final int HI = 69;
    public static final int HELLO = 420;
    public static final int DATA = 1;
    public static ShapesRenderer shapes = null;

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

    public static boolean sendClientCommand(String command)
    {
        if (!isServerCarpet && CarpetServer.minecraft_server == null) return false;
        ClientNetworkHandler.clientCommand(command);
        return true;
    }

    public static void onClientCommand(Tag t)
    {
        CarpetSettings.LOG.info("Server Response:");
        CompoundTag tag = (CompoundTag)t;
        CarpetSettings.LOG.info(" - id: "+tag.getString("id"));
        CarpetSettings.LOG.info(" - code: "+tag.getInt("code"));
        if (tag.contains("error")) CarpetSettings.LOG.warn(" - error: "+tag.getString("error"));
        if (tag.contains("output"))
        {
            ListTag outputTag = (ListTag) tag.get("output");
            for (int i = 0; i < outputTag.size(); i++)
                CarpetSettings.LOG.info(" - response: " + Text.Serializer.fromJson(outputTag.getString(i)).getString());
        }
    }
}
