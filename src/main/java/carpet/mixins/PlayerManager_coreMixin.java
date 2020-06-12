package carpet.mixins;

import carpet.CarpetServer;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static carpet.script.CarpetEventServer.Event.PLAYER_CONNECTS;

@Mixin(PlayerManager.class)
public class PlayerManager_coreMixin
{
    @Inject(method = "onPlayerConnect", at = @At("RETURN"))
    private void onPlayerConnected(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci)
    {
        CarpetServer.onPlayerLoggedIn(player);
        PLAYER_CONNECTS.onPlayerEvent(player);
    }
}
