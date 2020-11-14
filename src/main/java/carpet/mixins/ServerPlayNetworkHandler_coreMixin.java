package carpet.mixins;

import carpet.CarpetServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static carpet.script.CarpetEventServer.Event.PLAYER_DISCONNECTS;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandler_coreMixin {
    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "onDisconnected", at = @At("HEAD"))
    private void onPlayerDisconnect(Text reason, CallbackInfo ci) {
        CarpetServer.onPlayerLoggedOut(this.player);
        if (PLAYER_DISCONNECTS.isNeeded()) PLAYER_DISCONNECTS.onPlayerMessage(player, reason.asString());
    }
}
