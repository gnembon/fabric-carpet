package carpet.mixins;

import carpet.CarpetServer;
import carpet.fakes.ServerGamePacketListenerImplInterface;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImpl_coreMixin extends ServerCommonPacketListenerImpl implements ServerGamePacketListenerImplInterface {
    @Shadow
    public ServerPlayer player;

    public ServerGamePacketListenerImpl_coreMixin(final MinecraftServer minecraftServer, final Connection connection, final CommonListenerCookie i)
    {
        super(minecraftServer, connection, i);
    }

    @Inject(method = "onDisconnect", at = @At("HEAD"))
    private void onPlayerDisconnect(DisconnectionDetails reason, CallbackInfo ci) {
        CarpetServer.onPlayerLoggedOut(this.player, reason.reason());
    }

    @Override
    public Connection getConnection() {
        return connection;
    }
}
