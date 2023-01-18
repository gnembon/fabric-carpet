package carpet.mixins;

import carpet.CarpetServer;
import carpet.fakes.ServerGamePacketListenerImplInterface;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static carpet.script.CarpetEventServer.Event.PLAYER_DISCONNECTS;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImpl_coreMixin implements ServerGamePacketListenerImplInterface {
    @Shadow
    public ServerPlayer player;

    @Shadow @Final private Connection connection;

    @Inject(method = "onDisconnect", at = @At("HEAD"))
    private void onPlayerDisconnect(Component reason, CallbackInfo ci) {
        CarpetServer.onPlayerLoggedOut(this.player);
        if (PLAYER_DISCONNECTS.isNeeded()) PLAYER_DISCONNECTS.onPlayerMessage(player, reason.getContents().toString());
    }

    @Override
    public Connection getConnection() {
        return connection;
    }
}
