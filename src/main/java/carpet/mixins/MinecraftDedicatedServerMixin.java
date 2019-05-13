package carpet.mixins;

import carpet.CarpetServer;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftDedicatedServer.class)
public class MinecraftDedicatedServerMixin
{
    //to inject right before
    // this.tickWorlds(booleanSupplier_1);
    @Inject(
            method = "setupServer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/UserCache;setUseRemote(Z)V",
                    shift = At.Shift.AFTER,
                    ordinal = 0
            )
    )
    private void onSetupServer(CallbackInfoReturnable<Boolean> cir) {
        //CM init - all stuff loaded from the server, just before worlds loading
        CarpetServer.onServerLoaded((MinecraftDedicatedServer) (Object) this);
        //CM start game hook
        CarpetServer.onGameStarted();
    }
}
