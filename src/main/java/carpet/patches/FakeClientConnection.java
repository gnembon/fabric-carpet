package carpet.patches;

import carpet.fakes.ClientConnectionInterface;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;

public class FakeClientConnection extends Connection
{
    public FakeClientConnection(PacketFlow p)
    {
        super(p);
        // compat with adventure-platform-fabric. This does NOT trigger other vanilla handlers for establishing a channel
        // also makes #isOpen return true, allowing enderpearls to teleport fake players
        ((ClientConnectionInterface)this).setChannel(new EmbeddedChannel());
    }

    @Override
    public void setReadOnly()
    {
    }

    @Override
    public void handleDisconnection()
    {
    }
}