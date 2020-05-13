package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.class_5294;
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
            target = "Lnet/minecraft/class_5294;method_28110(II)Z"  //method_28110  isFogThick
    ))
    private boolean isReallyThick(class_5294 class_5294, int x, int z)
    {
        if (CarpetSettings.fogOff) return false;
        return class_5294.method_28110(x, z); //isFogThick
    }

}
