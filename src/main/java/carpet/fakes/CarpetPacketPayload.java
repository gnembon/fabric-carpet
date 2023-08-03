package carpet.fakes;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.ResourceLocation;

public interface CarpetPacketPayload extends CustomPacketPayload
{
    FriendlyByteBuf data();
}
