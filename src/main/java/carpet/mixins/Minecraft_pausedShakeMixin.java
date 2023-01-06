package carpet.mixins;

import carpet.fakes.MinecraftClientInferface;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Minecraft.class)
public class Minecraft_pausedShakeMixin implements MinecraftClientInferface
{
    @Shadow private float pausePartialTick;

    @Override
    public float getPausedTickDelta()
    {
        return pausePartialTick;
    }
}
