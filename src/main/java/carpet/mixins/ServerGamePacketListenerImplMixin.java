package carpet.mixins;

import carpet.network.CarpetClient;
import carpet.network.ServerNetworkHandler;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
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
            // We should force onto the main thread here
            // ServerNetworkHandler.handleData can possibly mutate data that isn't
            // thread safe, and also allows for client commands to be executed
            PacketUtils.ensureRunningOnSameThread(packet, (ServerGamePacketListener) this, player.getLevel());
            ServerNetworkHandler.handleData(packet.getData(), player);
            ci.cancel();
        }
    }
}
