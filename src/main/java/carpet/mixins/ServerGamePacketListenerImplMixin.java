package carpet.mixins;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
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
public abstract class ServerGamePacketListenerImplMixin extends ServerCommonPacketListenerImpl
{
    public ServerGamePacketListenerImplMixin(MinecraftServer server, Connection connection, CommonListenerCookie cookie)
    {
        super(server, connection, cookie);
    }

    @Shadow
    public abstract ServerPlayer getPlayer();

    @Inject(method = "onDisconnect", at = @At("TAIL"))
    private void createShadow(DisconnectionDetails details, CallbackInfo ci)
    {
        EntityPlayerMPFake.createShadow(this.server, this.getPlayer());
    }
}
