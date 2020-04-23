package carpet.mixins;

import carpet.CarpetServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.level.LevelGeneratorOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IntegratedServer.class)
public class IntegratedServerMixin
{
    @Inject(method = "setupServer", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/integrated/IntegratedServer;loadWorld()V",
            shift = At.Shift.BEFORE
    ))
    private void onSetupServerIntegrated(CallbackInfoReturnable<Boolean> cir) {
        CarpetServer.onServerLoaded((IntegratedServer) (Object) this);
    }
}
