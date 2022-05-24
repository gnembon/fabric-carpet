package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ServerPlayerGameMode.class, priority = 69420) // not that important for carpet
public class ServerPlayerGameMode_antiCheatMixin
{
    // that shoudn't've been a constant at the first place
    // resolves problems with mobs using reach entity attributes.

    @Redirect(method = "handleBlockBreakAction", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;getEyePosition()Lnet/minecraft/world/phys/Vec3;"
    ))
    private Vec3 getEyePos(ServerPlayer instance,
                           final BlockPos pos, final ServerboundPlayerActionPacket.Action action, final Direction direction, final int maxBuildHeight, final int sequence)
    {
        if (CarpetSettings.antiCheatDisabled &&
                instance.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) < 1024
        ) return Vec3.atCenterOf(pos);
        return instance.getEyePosition();
    }
}
