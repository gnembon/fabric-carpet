package carpet.mixins;

import carpet.network.CarpetClient;
import net.minecraft.client.render.debug.DebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DebugRenderer.class)
public class DebugRenderer_scarpetRenderMixin
{
    @Inject(method = "reset", at = @At("HEAD"))
    private void resetScarpetRenderes(CallbackInfo ci)
    {
        if (CarpetClient.shapes != null)
            CarpetClient.shapes.reset();
    }
}
