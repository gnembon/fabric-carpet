package carpet.mixins;

import carpet.network.CarpetClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerboundCustomPayloadPacket.class)
public class ServerboundCustomPayloadPacket_mixin
{
    @Inject(method = "readPayload", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/common/ServerboundCustomPayloadPacket;readUnknownPayload(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/network/FriendlyByteBuf;)Lnet/minecraft/network/protocol/common/custom/DiscardedPayload;"
    ), cancellable = true)
    private static void onOnCustomPayload(ResourceLocation resourceLocation, FriendlyByteBuf friendlyByteBuf, CallbackInfoReturnable<CustomPacketPayload> cir)
    {
        if (CarpetClient.CARPET_CHANNEL.equals(resourceLocation))
        {
            cir.setReturnValue(new CarpetClient.CarpetPayload(friendlyByteBuf));
        }
    }
}
