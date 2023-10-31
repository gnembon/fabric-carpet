package carpet.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CarpetPayload(int command, CompoundTag data) implements CustomPacketPayload
{
    public static final int DATA = 1; // The only command, in the packet for backwards compat

    public CarpetPayload(CompoundTag data)
    {
        this(DATA, data);
    }

    public CarpetPayload(FriendlyByteBuf input)
    {
        this(input.readInt(), input.readNbt());
    }

    @Override
    public void write(FriendlyByteBuf output)
    {
        output.writeInt(command);
        output.writeNbt(data);
    }

    @Override
    public ResourceLocation id()
    {
        return CarpetClient.CARPET_CHANNEL;
    }
}
