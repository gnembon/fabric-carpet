package carpet.fakes;

import net.minecraft.network.Connection;

public interface ServerGamePacketListenerImplInterface
{
    default Connection carpet$getConnection() { throw new UnsupportedOperationException(); }
}
