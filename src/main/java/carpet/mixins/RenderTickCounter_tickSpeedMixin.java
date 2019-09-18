package carpet.mixins;

import carpet.helpers.TickSpeed;
import carpet.settings.CarpetSettings;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderTickCounter.class)
public class RenderTickCounter_tickSpeedMixin {
    @Redirect(method = "beginRenderTick", at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/render/RenderTickCounter;timeScale:F"
    ))
    private float adjustTickSpeed(RenderTickCounter counter) {
        if (CarpetSettings.smoothClientAnimations && TickSpeed.process_entities)
        {
            return Math.max(50.0f, TickSpeed.mspt);
        }
        return 50f;
    }
}