package carpet.mixins;

import carpet.helpers.CarpetTaintedList;
import carpet.network.CarpetClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(CustomPacketPayload.class)
public interface CustomPacketPayload_networkStuffMixin
{
    @Inject(method = "codec(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload$FallbackProvider;Ljava/util/List;)Lnet/minecraft/network/codec/StreamCodec;", at = @At("HEAD"), cancellable = true)
    private static <B extends FriendlyByteBuf> void onCodec(final CustomPacketPayload.FallbackProvider<B> fallbackProvider, final List<CustomPacketPayload.TypeAndCodec<? super B, ?>> list, final CallbackInfoReturnable<StreamCodec<B, CustomPacketPayload>> cir)
    {
        // this is stupid hack to make sure carpet payloads are always registered
        // that might collide with other mods that do the same thing
        // so we may need to adjust this in the future
        if (!(list instanceof CarpetTaintedList))
        {
            List<CustomPacketPayload.TypeAndCodec<? super B, ?>> extendedList = new CarpetTaintedList<>(list);
            extendedList.add(new CustomPacketPayload.TypeAndCodec<>(CarpetClient.CarpetPayload.TYPE, CarpetClient.CarpetPayload.STREAM_CODEC));
            cir.setReturnValue(CustomPacketPayload.codec(fallbackProvider, extendedList));
        }
    }
}
