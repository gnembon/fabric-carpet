package carpet.mixins;


import carpet.network.CarpetClient;
import carpet.network.ClientNetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientboundCustomPayloadPacket.class)
public class ClientboundCustomPayloadPacket_customPacketMixin
{
    @Inject(method = "readUnknownPayload", at = @At("HEAD"), cancellable = true)
    private static void onOnCustomPayloadR(ResourceLocation resourceLocation, FriendlyByteBuf friendlyByteBuf, CallbackInfoReturnable<CustomPacketPayload> cir)
    {
        if (resourceLocation.equals(CarpetClient.CARPET_CHANNEL))
        {
            cir.setReturnValue(new CarpetClient.CarpetPayload(friendlyByteBuf));
        }
    }
}
