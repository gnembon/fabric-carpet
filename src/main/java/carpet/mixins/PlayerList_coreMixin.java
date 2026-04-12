package carpet.mixins;

import carpet.CarpetServer;
import carpet.network.ServerNetworkHandler;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class PlayerList_coreMixin
{

    @Inject(method = "placeNewPlayer", at = @At("RETURN"))
    private void onPlayerConnected(Connection connection, ServerPlayer player, CommonListenerCookie i, CallbackInfo ci)
    {
        CarpetServer.onPlayerLoggedIn(player);
    }

    @Inject(method = "sendLevelInfo", at = @At("RETURN"))
    private void onLevelChanged(final ServerPlayer serverPlayer, final ServerLevel serverLevel, final CallbackInfo ci)
    {
        ServerNetworkHandler.sendPlayerLevelData(serverPlayer, serverLevel);
    }
}
