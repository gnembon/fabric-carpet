package carpet.mixins;

import carpet.fakes.CarpetPacketPayload;
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
        if (true) return;
        Object thiss = this;
        if (thiss instanceof ServerGamePacketListenerImpl impl && serverboundCustomPayloadPacket.payload() instanceof CarpetPacketPayload cpp) {
            // We should force onto the main thread here
            // ServerNetworkHandler.handleData can possibly mutate data that isn't
            // thread safe, and also allows for client commands to be executed
            PacketUtils.ensureRunningOnSameThread(serverboundCustomPayloadPacket, (ServerGamePacketListener) this, impl.player.serverLevel());
            ServerNetworkHandler.handleData(cpp.data(), impl.player);
            ci.cancel();
        }
        ci.cancel();
    }
}
