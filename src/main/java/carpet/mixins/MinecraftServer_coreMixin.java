package carpet.mixins;

import carpet.CarpetServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.LevelGeneratorOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServer_coreMixin
{
    //to inject right before
    // this.tickWorlds(booleanSupplier_1);
    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;tickWorlds(Ljava/util/function/BooleanSupplier;)V",
                    shift = At.Shift.BEFORE,
                    ordinal = 0
            )
    )
    private void onTick(BooleanSupplier booleanSupplier_1, CallbackInfo ci) {
        CarpetServer.tick((MinecraftServer) (Object) this);
    }

    // Dedicated server only
    @Inject(method = "loadWorld", at = @At("HEAD"))
    private void serverLoaded(String name, String serverName, long seed, LevelGeneratorOptions arg, CallbackInfo ci)
    {
        CarpetServer.onServerLoaded((MinecraftServer) (Object) this);
    }

    @Inject(method = "shutdown", at = @At("HEAD"))
    private void serverClosed(CallbackInfo ci)
    {
        CarpetServer.onServerClosed((MinecraftServer) (Object) this);
    }
}
