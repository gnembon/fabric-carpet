package carpet.network;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.script.utils.ShapesRenderer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class CarpetClient
{
    public static final int HI = 69;
    public static final int HELLO = 420;
    public static final int DATA = 1;
    public static ShapesRenderer shapes = null;

    private static LocalPlayer clientPlayer = null;
    private static boolean isServerCarpet = false;
    public static String serverCarpetVersion;
    public static final ResourceLocation CARPET_CHANNEL = new ResourceLocation("carpet:hello");

    public static void gameJoined(LocalPlayer player)
    {
        clientPlayer = player;
    }

    public static void disconnect()
    {
        if (isServerCarpet) // multiplayer connection
        {
            isServerCarpet = false;
            clientPlayer = null;
            CarpetServer.onServerClosed(null);
            CarpetServer.onServerDoneClosing(null);
        }
        else // singleplayer disconnect
        {
            CarpetServer.clientPreClosing();
        }
    }

    public static void setCarpet()
    {
        isServerCarpet = true;
    }

    public static LocalPlayer getPlayer()
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
                CarpetSettings.LOG.info(" - response: " + Component.Serializer.fromJson(outputTag.getString(i)).getString());
        }
    }
}
