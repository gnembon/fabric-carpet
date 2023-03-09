package carpet.patches;

import carpet.mixins.ServerGamePacketListenerImplAccessor;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.RelativeMovement;
import java.util.Set;

public class NetHandlerPlayServerFake extends ServerGamePacketListenerImpl
{
    public NetHandlerPlayServerFake(MinecraftServer server, Connection cc, EntityPlayerMPFake playerIn)
    {
        super(server, cc, playerIn);
    }

    @Override
    public void send(final Packet<?> packetIn)
    {
    }

    @Override
    public void disconnect(Component message)
    {
        if (message.getContents() instanceof TranslatableContents text && (text.getKey().equals("multiplayer.disconnect.idling") || text.getKey().equals("multiplayer.disconnect.duplicate_login")))
        {
            ((EntityPlayerMPFake) player).kill(message);
        }
    }
    private boolean hasSpawned = false;

    @Override
    public void teleport(double d, double e, double f, float g, float h, Set<RelativeMovement> set)
    {
        super.teleport(d, e, f, g, h, set);

        handleAcceptTeleportPacket(
            new ServerboundAcceptTeleportationPacket( ((ServerGamePacketListenerImplAccessor)this).getAwaitingTeleport() )
        );

        if (!hasSpawned) {
            hasSpawned = true;
        } else {
            handleMovePlayer(
                new ServerboundMovePlayerPacket.PosRot(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot(), false)
            );
        }
    }

}



