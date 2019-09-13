package carpet.mixins;

import carpet.helpers.TickSpeed;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MinecraftClient.class)
public class MinecraftClient_pausedShakeMixin
{
    @Shadow private boolean paused;

    @Redirect(method =  "render", at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/MinecraftClient;paused:Z",
            ordinal = 0
    ))
    private boolean isPausedForRendering(MinecraftClient client)
    {
        return paused || !TickSpeed.process_entities;
    }
}
