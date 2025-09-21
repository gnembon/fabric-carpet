package carpet.patches;

import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import java.util.Set;

public class NetHandlerPlayServerFake extends ServerGamePacketListenerImpl
{
    public NetHandlerPlayServerFake(final MinecraftServer minecraftServer, final Connection connection, final ServerPlayer serverPlayer, final CommonListenerCookie i)
    {
        super(minecraftServer, connection, serverPlayer, i);
    }

    @Override
    public void send(final Packet<?> packetIn)
    {
    }

    @Override
    public void disconnect(Component message)
    {
        if (message.getContents() instanceof TranslatableContents text && text.getKey().equals("multiplayer.disconnect.not_whitelisted"))
        {
            // Whitelist stuff triggers this in multiple places and we shouldn't let fake players randomly disconnect
            // as we don't expect them to be whitelisted
            return;
        }
        ((EntityPlayerMPFake) player).kill(message);
    }

    @Override
    public void teleport(PositionMoveRotation positionMoveRotation, Set<Relative> set)
    {
        super.teleport(positionMoveRotation, set);
        if (player.level().getPlayerByUUID(player.getUUID()) != null) {
            resetPosition();
            player.level().getChunkSource().move(player);
        }
    }

}
