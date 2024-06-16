package carpet.mixins;

import carpet.network.CarpetClient;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonPacketListenerImpl.class)
public class ClientCommonPacketListenerImpl_customPacketMixin
{
    @Inject(method = "onDisconnect", at = @At("HEAD"))
    private void onCMDisconnected(DisconnectionDetails reason, CallbackInfo ci)
    {
        CarpetClient.disconnect();
    }

    @Inject(method = "handleCustomPayload(Lnet/minecraft/network/protocol/common/ClientboundCustomPayloadPacket;)V",
    at = @At("HEAD"))
    private void onOnCustomPayload(ClientboundCustomPayloadPacket packet, CallbackInfo ci)
    {
        //System.out.println("CustomPayload of : " + packet.payload());
    }

}
