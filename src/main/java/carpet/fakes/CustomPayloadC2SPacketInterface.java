package carpet.fakes;

import net.minecraft.util.Identifier;
import net.minecraft.network.PacketByteBuf;

public interface CustomPayloadC2SPacketInterface
{
    Identifier getPacketChannel();
    PacketByteBuf getPacketData();
}
