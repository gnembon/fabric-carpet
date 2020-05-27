package carpet.mixins;

import carpet.CarpetServer;
import carpet.helpers.ClientNetworkHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.PacketByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin
{
    @Inject(method = "onCustomPayload", at = @At(value = "CONSTANT", args = "stringValue=Unknown custom packed identifier: {}"), cancellable = true, locals = LocalCapture.CAPTURE_FAILSOFT, require = 0)
    private void onCustomPayloadNotFound(CustomPayloadS2CPacket packet, CallbackInfo ci, Identifier id, PacketByteBuf buf)
    {
        if (CarpetServer.CARPET_CHANNEL.equals(id))
        {
            if (buf.refCnt() > 0)
            {
                buf.release();
            }
            
            ci.cancel();
        }
    }
    
    @Inject(method = "onCustomPayload", at = @At("HEAD"), cancellable = true)
    private void onOnCustomPayload(CustomPayloadS2CPacket packet, CallbackInfo ci)
    {
        Identifier channel = packet.getChannel();
        PacketByteBuf buf = packet.getData();
        ClientNetworkHandler.handleData(channel, buf);
        ci.cancel();
    }
}
