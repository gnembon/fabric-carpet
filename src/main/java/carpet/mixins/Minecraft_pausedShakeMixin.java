package carpet.mixins;

import carpet.fakes.MinecraftInterface;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Minecraft.class)
public class Minecraft_pausedShakeMixin implements MinecraftInterface
{
    @Shadow private float pausePartialTick;

    @Override
    public float carpet$getPausePartialTick()
    {
        return pausePartialTick;
    }
}
