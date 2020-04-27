package carpet.mixins;

import carpet.helpers.TickSpeed;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandler_tickMixin
{
    @Shadow public ServerPlayerEntity player;

    @Shadow private double lastTickX;

    @Shadow private double lastTickY;

    @Shadow private double lastTickZ;

    @Inject(method = "onPlayerInput", at = @At(value = "RETURN"))
    private void checkMoves(PlayerInputC2SPacket p, CallbackInfo ci)
    {
        if (p.getSideways() != 0.0F || p.getForward() != 0.0F || p.isJumping() || p.isSneaking())
        {
            TickSpeed.reset_player_active_timeout();
        }
    }

    // to skip reposition adjustment check
    private static long lastMovedTick = 0L;
    private static double lastMoved = 0.0D;

    @Inject(method = "onPlayerMove",  at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerEntity;isSleeping()Z",
            shift = At.Shift.BEFORE
    ))
    private void checkMove(PlayerMoveC2SPacket p, CallbackInfo ci)
    {
        double movedBy = player.getPos().squaredDistanceTo(lastTickX, lastTickY, lastTickZ);
        if (movedBy == 0.0D) return;
        // corrective tick
        if (movedBy < 0.0009 && lastMoved > 0.0009 && Math.abs(player.getServer().getTicks()-lastMovedTick-20)<2)
        {
            //CarpetSettings.LOG.error("Corrective movement packet");
            return;
        }
        if (movedBy > 0.0D)
        {
            //CarpetSettings.LOG.error(String.format(
            //        "moved by %.6f at %d",
            //        player.getPos().squaredDistanceTo(lastTickX, lastTickY, lastTickZ),
            //        player.getServer().getTicks()-lastMovedTick
            //));
            lastMoved = movedBy;
            lastMovedTick = player.getServer().getTicks();
            TickSpeed.reset_player_active_timeout();
        }
    }
}
