package carpet.mixins;

import carpet.helpers.TickSpeed;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImpl_tickMixin
{
    @Shadow public ServerPlayer player;

    @Shadow private double firstGoodX;

    @Shadow private double firstGoodY;

    @Shadow private double firstGoodZ;

    @Inject(method = "handlePlayerInput", at = @At(value = "RETURN"))
    private void checkMoves(ServerboundPlayerInputPacket p, CallbackInfo ci)
    {
        if (p.getXxa() != 0.0F || p.getZza() != 0.0F || p.isJumping() || p.isShiftKeyDown())
        {
            TickSpeed.reset_player_active_timeout();
        }
    }

    // to skip reposition adjustment check
    private static long lastMovedTick = 0L;
    private static double lastMoved = 0.0D;

    @Inject(method = "handleMovePlayer",  at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;isSleeping()Z",
            shift = At.Shift.BEFORE
    ))
    private void checkMove(ServerboundMovePlayerPacket p, CallbackInfo ci)
    {
        double movedBy = player.position().distanceToSqr(firstGoodX, firstGoodY, firstGoodZ);
        if (movedBy == 0.0D) return;
        // corrective tick
        if (movedBy < 0.0009 && lastMoved > 0.0009 && Math.abs(player.getServer().getTickCount()-lastMovedTick-20)<2)
        {
            return;
        }
        if (movedBy > 0.0D)
        {
            lastMoved = movedBy;
            lastMovedTick = player.getServer().getTickCount();
            TickSpeed.reset_player_active_timeout();
        }
    }
}
