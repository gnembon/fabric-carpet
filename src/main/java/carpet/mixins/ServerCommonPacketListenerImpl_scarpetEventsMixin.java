package carpet.mixins;

import carpet.script.CarpetEventServer;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerPlayerConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ServerCommonPacketListenerImpl_scarpetEventsMixin
{
    @Inject(
            method = "handleCustomClickAction",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;handleCustomClickAction(Lnet/minecraft/resources/ResourceLocation;Ljava/util/Optional;)V"
            ),
            cancellable = true
    )
    private void onCustomClickAction(ServerboundCustomClickActionPacket serverboundCustomClickActionPacket, CallbackInfo ci)
    {
        if(CarpetEventServer.Event.PLAYER_CUSTOM_CLICK_ACTION.isNeeded() && this instanceof ServerPlayerConnection serverPlayerConnection)
        {
            boolean cancel = CarpetEventServer.Event.PLAYER_CUSTOM_CLICK_ACTION.onPlayerCustomClickAction(
                    serverPlayerConnection.getPlayer(),
                    serverboundCustomClickActionPacket.id(),
                    serverboundCustomClickActionPacket.payload().orElse(null)
            );
            if(cancel) ci.cancel();
        }
    }
}
