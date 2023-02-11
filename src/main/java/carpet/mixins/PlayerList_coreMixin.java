package carpet.mixins;

import carpet.CarpetServer;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class PlayerList_coreMixin
{

    @Inject(method = "placeNewPlayer", at = @At("RETURN"))
    private void onPlayerConnected(Connection connection, ServerPlayer player, CallbackInfo ci)
    {
        CarpetServer.onPlayerLoggedIn(player);
    }
}
