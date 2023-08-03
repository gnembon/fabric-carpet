package carpet.mixins;

import carpet.fakes.CarpetPacketPayload;
import carpet.network.CarpetClient;
import carpet.network.ClientNetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
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
        if (true) return;
        if (CarpetClient.CARPET_CHANNEL.equals(resourceLocation))
        {
            //ClientNetworkHandler.handleData(friendlyByteBuf, Minecraft.getInstance().player);
            cir.setReturnValue(new CarpetPacketPayload()
            {
                @Override
                public FriendlyByteBuf data()
                {
                    return friendlyByteBuf;
                }

                @Override
                public void write(final FriendlyByteBuf friendlyByteBuf)
                {
                    // noop - its a client side and we handle it server side only
                }

                @Override
                public ResourceLocation id()
                {
                    return resourceLocation;
                }
            });
        }
    }
}
