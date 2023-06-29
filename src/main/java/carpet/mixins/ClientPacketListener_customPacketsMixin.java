package carpet.mixins;

import carpet.network.CarpetClient;
import carpet.network.ClientNetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListener_customPacketsMixin
{
    @Final @Shadow private Minecraft minecraft;

    @Inject(method = "handleCustomPayload", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ClientboundCustomPayloadPacket;getIdentifier()Lnet/minecraft/resources/ResourceLocation;"), cancellable = true)
    private void onOnCustomPayload(ClientboundCustomPayloadPacket packet, CallbackInfo ci)
    {
        if (CarpetClient.CARPET_CHANNEL.equals(packet.getIdentifier()))
        {
            ClientNetworkHandler.handleData(packet.getData(), minecraft.player);
            ci.cancel();
        }
    }

    @Inject(method = "handleLogin", at = @At("RETURN"))
    private void onGameJoined(ClientboundLoginPacket packet, CallbackInfo info)
    {
        CarpetClient.gameJoined(minecraft.player);
    }

    @Inject(method = "onDisconnect", at = @At("HEAD"))
    private void onCMDisconnected(Component reason, CallbackInfo ci)
    {
        CarpetClient.disconnect();
    }
}
