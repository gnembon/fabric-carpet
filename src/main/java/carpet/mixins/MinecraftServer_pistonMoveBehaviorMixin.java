package carpet.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import carpet.helpers.PistonMoveBehaviorManager;

import net.minecraft.server.MinecraftServer;

@Mixin(MinecraftServer.class)
public class MinecraftServer_pistonMoveBehaviorMixin {

    @Inject(
        method = "saveEverything",
        at = @At(
            value = "HEAD"
        )
    )
    private void savePistonMoveBehaviorOverrides(CallbackInfo ci) {
        PistonMoveBehaviorManager.save((MinecraftServer)(Object)this);
    }
}
