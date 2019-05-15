package carpet.mixins;

import carpet.logging.logHelpers.PacketCounter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin
{
    // Add to the packet counter whenever a packet is received.
    @Inject(method = "method_10770", at = @At("HEAD"))
    private void packetInCount(ChannelHandlerContext channelHandlerContext_1, Packet<?> packet_1, CallbackInfo ci)
    {
        PacketCounter.totalIn++;
    }
    
    // Add to the packet counter whenever a packet is sent.
    @Inject(method = "sendImmediately", at = @At("HEAD"))
    private void packetOutCount(Packet<?> packet_1,
            GenericFutureListener<? extends Future<? super Void>> genericFutureListener_1, CallbackInfo ci)
    {
        PacketCounter.totalOut++;
    }
}
