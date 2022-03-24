package carpet.mixins;

import carpet.helpers.TickSpeed;
import net.minecraft.client.Timer;
import carpet.CarpetSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Timer.class)
public class Timer_tickSpeedMixin {
    @Redirect(method = "advanceTime", at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/Timer;msPerTick:F"
    ))
    private float adjustTickSpeed(Timer counter) {
        if (CarpetSettings.smoothClientAnimations && TickSpeed.process_entities)
        {
            return Math.max(50.0f, TickSpeed.mspt);
        }
        return 50f;
    }
}