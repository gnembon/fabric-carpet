package carpet.mixins;

import carpet.CarpetServer;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin
{
    //to inject right before
    // this.tickWorlds(booleanSupplier_1);
    @Inject(method = "run", at = @At(value = "HEAD")
    )
    private void onInit(CallbackInfo ci) {
        //CM start game hook
        CarpetServer.onGameStarted();
    }
}
