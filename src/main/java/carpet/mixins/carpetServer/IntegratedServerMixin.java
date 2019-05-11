package carpet.mixins.carpetServer;

import carpet.CarpetServer;
import net.minecraft.server.integrated.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IntegratedServer.class)
public class IntegratedServerMixin
{
    @Inject(method = "setupServer",
            at = @At(
                    value = "INVOKE",
                    shift = At.Shift.AFTER,
                    target = "Lnet/minecraft/server/integrated/IntegratedServer;setKeyPair(Ljava/security/KeyPair;)V"
            )
    )
    private void onSetupServerIntegrated(CallbackInfoReturnable<Boolean> cir) {
        CarpetServer.onServerLoaded((IntegratedServer) (Object) this);
    }
}
