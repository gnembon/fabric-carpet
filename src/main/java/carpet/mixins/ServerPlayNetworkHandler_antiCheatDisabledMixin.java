package carpet.mixins;

import carpet.settings.CarpetSettings;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandler_antiCheatDisabledMixin
{
    @Shadow private int floatingTicks;

    @Shadow private int vehicleFloatingTicks;

    @Shadow protected abstract boolean isServerOwner();

    @Shadow @Final public ClientConnection client;

    @Shadow public ServerPlayerEntity player;

    @Inject(method = "tick", at = @At("HEAD"))
    private void restrictFloatingBits(CallbackInfo ci)
    {
        if (CarpetSettings.antiCheatDisabled)
        {
            if (floatingTicks > 70) floatingTicks--;
            if (vehicleFloatingTicks > 70) vehicleFloatingTicks--;
        }

    }

    @Redirect(method = "onVehicleMove", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;isServerOwner()Z"
    ))
    private boolean isServerTrusting(ServerPlayNetworkHandler serverPlayNetworkHandler)
    {
        return isServerOwner() || CarpetSettings.antiCheatDisabled;
    }

    @Redirect(method = "onPlayerMove", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerEntity;isInTeleportationState()Z"))
    private boolean relaxMoveRestrictions(ServerPlayerEntity serverPlayerEntity)
    {
        return CarpetSettings.antiCheatDisabled || serverPlayerEntity.isInTeleportationState();
    }

    @Redirect(method = "onClientCommand", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerEntity;getVelocity()Lnet/minecraft/util/math/Vec3d;"
    ))
    private Vec3d getFallingVelocity(ServerPlayerEntity serverPlayerEntity)
    {
        if (CarpetSettings.antiCheatDisabled)
            return new Vec3d(0,-1,0);
        return serverPlayerEntity.getVelocity();
    }

    @Redirect(method = "onClientCommand", at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/server/network/ServerPlayerEntity;onGround:Z"
    ))
    private boolean getNotOnGround(ServerPlayerEntity serverPlayerEntity)
    {
        if (CarpetSettings.antiCheatDisabled)
            return false;
        return serverPlayerEntity.onGround;
    }
}
