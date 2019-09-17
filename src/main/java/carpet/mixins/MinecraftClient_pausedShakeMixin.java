package carpet.mixins;

import carpet.fakes.MinecraftClientInferface;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MinecraftClient.class)
public class MinecraftClient_pausedShakeMixin implements MinecraftClientInferface
{
    @Shadow private float pausedTickDelta;

    @Override
    public float getPausedTickDelta()
    {
        return pausedTickDelta;
    }
}
