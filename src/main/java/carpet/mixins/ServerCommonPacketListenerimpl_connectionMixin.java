package carpet.mixins;

import carpet.CarpetSettings;
import carpet.network.CarpetPayload;
import carpet.network.ServerNetworkHandler;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerCommonPacketListenerImpl.class)
public class ServerCommonPacketListenerimpl_connectionMixin
{
    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    private void onCustomCarpetPayload(ServerboundCustomPayloadPacket serverboundCustomPayloadPacket, CallbackInfo ci)
    {
        Object thiss = this;
        if (thiss instanceof ServerGamePacketListenerImpl impl && serverboundCustomPayloadPacket.payload() instanceof CarpetPayload cpp) {
            // We should force onto the main thread here
            // ServerNetworkHandler.handleData can possibly mutate data that isn't
            // thread safe, and also allows for client commands to be executed
            PacketUtils.ensureRunningOnSameThread(serverboundCustomPayloadPacket, (ServerGamePacketListener) this, impl.player.serverLevel());
            if (cpp.command() == CarpetPayload.DATA) {
                ServerNetworkHandler.onClientData(impl.player, cpp.data());
            } else {
                CarpetSettings.LOG.info("Invalid carpet-like packet received");
            }
            ci.cancel();
        }
    }
}
