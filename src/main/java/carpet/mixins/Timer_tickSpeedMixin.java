package carpet.mixins;

import carpet.fakes.MinecraftInterface;
import carpet.helpers.TickRateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Timer;
import carpet.CarpetSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

@Mixin(Timer.class)
public class Timer_tickSpeedMixin {
    @Redirect(method = "advanceTime", at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/Timer;msPerTick:F"
    ))
    private float adjustTickSpeed(Timer counter) {
        if (CarpetSettings.smoothClientAnimations)
        {
            Optional<TickRateManager> trm = ((MinecraftInterface)Minecraft.getInstance()).getTickRateManager();
            if (trm.isPresent() && trm.get().runsNormally())
            {
                return Math.max(50.0f, trm.get().mspt());
            }
        }
        return 50f;
    }
}