package carpet.patches;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

public class NetHandlerPlayServerFake extends ServerPlayNetworkHandler
{
    public NetHandlerPlayServerFake(MinecraftServer server, ClientConnection cc, EntityPlayerMPFake playerIn)
    {
        super(server, cc, playerIn);
    }

    @Override
    public void sendPacket(final Packet<?> packetIn)
    {
    }

    @Override
    public void disconnect(Text message)
    {
        if (message instanceof TranslatableText text && (text.getKey().equals("multiplayer.disconnect.idling") || text.getKey().equals("multiplayer.disconnect.duplicate_login")))
        {
            ((EntityPlayerMPFake) player).kill(message);
        }
    }
}



