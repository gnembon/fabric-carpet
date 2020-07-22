package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ServerPlayerInteractionManager.class, priority = 69420) // not that important for carpet
public class ServerPlayerInteractionManager_antiCheatMixin
{
    /*
    @ModifyConstant(method = "processBlockBreakingAction", require = 0,
            constant = @Constant(doubleValue = 36D))
    private double addDistance(double original) {
        if (CarpetSettings.antiCheatDisabled)
            return 1024D; // blocks 32 distance
        return original;
    }
    */

    // that shoudn't've been a constant at the first place
    // resolves problems with mobs using reach entity attributes.

    @Redirect(method = "processBlockBreakingAction", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerEntity;getX()D"
            ))
    private double getXX(ServerPlayerEntity player,
                         BlockPos pos, PlayerActionC2SPacket.Action action, Direction direction, int worldHeight)
    {
        if (CarpetSettings.antiCheatDisabled &&
                player.getPos().add(-0.5, 1.0, -0.5).squaredDistanceTo(new Vec3d(pos)) < 1024
        ) return pos.getX()+0.5;
        return player.getX();
    }

    @Redirect(method = "processBlockBreakingAction", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerEntity;getY()D"
    ))
    private double getYY(ServerPlayerEntity player,
                         BlockPos pos, PlayerActionC2SPacket.Action action, Direction direction, int worldHeight)
    {
        if (CarpetSettings.antiCheatDisabled &&
                player.getPos().add(-0.5, 1.0, -0.5).squaredDistanceTo(new Vec3d(pos)) < 1024
        ) return pos.getY()-1.0;
        return player.getY();
    }

    @Redirect(method = "processBlockBreakingAction", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerEntity;getZ()D"
    ))
    private double getZZ(ServerPlayerEntity player,
                         BlockPos pos, PlayerActionC2SPacket.Action action, Direction direction, int worldHeight)
    {
        if (CarpetSettings.antiCheatDisabled &&
                player.getPos().add(-0.5, 1.0, -0.5).squaredDistanceTo(new Vec3d(pos)) < 1024
        ) return pos.getZ()+0.5;
        return player.getZ();
    }


}
