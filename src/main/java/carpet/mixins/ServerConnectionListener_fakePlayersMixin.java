package carpet.mixins;

import carpet.CarpetSettings;
import carpet.fakes.ServerPlayerInterface;
import carpet.patches.FakePlayerManager;
import net.minecraft.server.network.ServerConnectionListener;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerConnectionListener.class)
public class ServerConnectionListener_fakePlayersMixin {
    @Inject(
            method = "tick",
            at = @At("RETURN")
    )
    private void tickFakePlayers(CallbackInfo ci) {
        if (!CarpetSettings.fakePlayerTicksInEU) {
            for (ServerGamePacketListenerImpl connection : FakePlayerManager.connections) {
                // from ServerGamePacketListenerImpl#tick

                connection.player.xo = connection.player.getX();
                connection.player.yo = connection.player.getY();
                connection.player.zo = connection.player.getZ();

                // action packet update
                ((ServerPlayerInterface) connection.player).getActionPack().onUpdate();

                connection.player.doTick();
                //  connection.player.absMoveTo(connection.firstGoodX, connection.firstGoodY, connection.firstGoodZ, connection.player.getYRot(), connection.player.getXRot());

                // todo: vehicle?
            }
        }
    }
}
