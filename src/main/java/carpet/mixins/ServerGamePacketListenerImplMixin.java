package carpet.mixins;

import carpet.network.CarpetClient;
import carpet.network.ServerNetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin
{
    @Shadow public ServerPlayer player;

    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    private void onCustomCarpetPayload(ServerboundCustomPayloadPacket packet, CallbackInfo ci)
    {
        ResourceLocation channel = packet.getIdentifier();
        if (CarpetClient.CARPET_CHANNEL.equals(channel))
        {
            ServerNetworkHandler.handleData(new FriendlyByteBuf(packet.getData().copy()), player);
            ci.cancel();
        }
    }
}
