package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImpl_antiCheatDisabledMixin
{
    @Shadow private int aboveGroundTickCount;

    @Shadow private int aboveGroundVehicleTickCount;

    @Shadow protected abstract boolean isSingleplayerOwner();

    @Inject(method = "tick", at = @At("HEAD"))
    private void restrictFloatingBits(CallbackInfo ci)
    {
        if (CarpetSettings.antiCheatDisabled)
        {
            if (aboveGroundTickCount > 70) aboveGroundTickCount--;
            if (aboveGroundVehicleTickCount > 70) aboveGroundVehicleTickCount--;
        }

    }

    @ModifyExpressionValue(method = "handleMoveVehicle", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;isSingleplayerOwner()Z"
    ))
    private boolean isServerTrusting(boolean original)
    {
        return original || CarpetSettings.antiCheatDisabled;
    }

    @ModifyExpressionValue(method = "handleMovePlayer",
             at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;isChangingDimension()Z"))
    private boolean relaxMoveRestrictions(boolean original)
    {
        return original || CarpetSettings.antiCheatDisabled;
    }
}
