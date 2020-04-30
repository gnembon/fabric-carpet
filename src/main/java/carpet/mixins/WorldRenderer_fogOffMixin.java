package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.world.dimension.Dimension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = WorldRenderer.class, priority = 69420)
public class WorldRenderer_fogOffMixin
{
    @Redirect(method = "render", require = 0, expect = 0, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/dimension/Dimension;isFogThick(II)Z"
    ))
    private boolean isReallyThick(Dimension dimension, int x, int z)
    {
        if (CarpetSettings.fogOff) return false;
        return dimension.isFogThick(x, z);
    }

}
