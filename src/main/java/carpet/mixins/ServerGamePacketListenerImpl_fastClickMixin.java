package carpet.mixins;

import carpet.fakes.ServerPlayerFastClickInterface;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImpl_fastClickMixin
{
    @Shadow
    public ServerPlayer player;

    private long _tick_seen;

    @Inject(method = "handleMovePlayer", at = @At("HEAD"))
    private void beforeHandleMovePlayer(ServerboundMovePlayerPacket serverboundMovePlayerPacket, CallbackInfo ci)
    {
        // Only for real players
        if (player instanceof EntityPlayerMPFake) {
            return;
        }
        // and only the first handleMovePlayer (per player) in a tick
        var _this_tick = player.serverLevel().getGameTime();
        if (_tick_seen == _this_tick) {
            return;
        }
        _tick_seen = _this_tick;
        ((ServerPlayerFastClickInterface)player).saveOldPosRot(player.position(), player.getRotationVector());
    }

}
