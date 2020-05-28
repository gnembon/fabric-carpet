package carpet.fakes;

import net.minecraft.util.Identifier;
import net.minecraft.util.PacketByteBuf;

public interface CustomPayloadC2SPacketInterface
{
    Identifier getPacketChannel();
    PacketByteBuf getPacketData();
}
