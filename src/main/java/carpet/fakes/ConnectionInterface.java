package carpet.fakes;

import io.netty.channel.Channel;

public interface ConnectionInterface
{
    default void setChannel(Channel channel) { throw new UnsupportedOperationException(); } // compat with adventure-platform-fabric
}
