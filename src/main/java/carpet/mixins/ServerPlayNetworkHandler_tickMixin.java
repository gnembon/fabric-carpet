package carpet.mixins;

import carpet.helpers.TickSpeed;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.packet.PlayerInputC2SPacket;
import net.minecraft.server.network.packet.PlayerMoveC2SPacket;
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

    @Inject(method = "onPlayerInput", at = @At(value = "RETURN"))
    private void checkMoves(PlayerInputC2SPacket p, CallbackInfo ci)
    {
        if (p.getSideways() != 0.0F || p.getForward() != 0.0F || p.isJumping() || p.isSneaking())
        {
            TickSpeed.reset_player_active_timeout();
        }
    }

    @Inject(method = "onPlayerMove",  at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerEntity;isSleeping()Z",
            shift = At.Shift.BEFORE
    ))
    private void checkMove(PlayerMoveC2SPacket p, CallbackInfo ci)
    {
        if (Math.abs(p.getX(player.x) - lastTickX) > 0.0001D
                || Math.abs(p.getY(player.y) - lastTickY) > 0.0001D
                || Math.abs(p.getY(player.y) - lastTickY) > 0.0001D)
        {
            TickSpeed.reset_player_active_timeout();
        }
    }
}
