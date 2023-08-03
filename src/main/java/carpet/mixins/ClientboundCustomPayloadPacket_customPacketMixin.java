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
    @Inject(method = "readPayload", at = @At("HEAD"))
    private static void onOnCustomPayloadR(final ResourceLocation resourceLocation, final FriendlyByteBuf friendlyByteBuf, final CallbackInfoReturnable<CustomPacketPayload> cir)
    {
    }

    @Inject(method = "readUnknownPayload", at = @At(value = "HEAD"), cancellable = true)
    private static void onOnCustomPayload(ResourceLocation resourceLocation, FriendlyByteBuf friendlyByteBuf, CallbackInfoReturnable<DiscardedPayload> cir)
    {
        if (true) return;
        if (CarpetClient.CARPET_CHANNEL.equals(resourceLocation) && Minecraft.getInstance().isSameThread())
        {
            ClientNetworkHandler.handleData(friendlyByteBuf, Minecraft.getInstance().player);
            cir.setReturnValue(new DiscardedPayload(resourceLocation));
        }
    }
}
