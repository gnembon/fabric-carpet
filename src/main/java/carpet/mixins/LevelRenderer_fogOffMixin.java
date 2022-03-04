package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LevelRenderer;
//import net.minecraft.world.dimension.Dimension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = LevelRenderer.class, priority = 69420)
public class LevelRenderer_fogOffMixin
{
    @Redirect(method = "renderLevel", require = 0, expect = 0, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/DimensionSpecialEffects;isFoggyAt(II)Z"
    ))
    private boolean isReallyThick(DimensionSpecialEffects skyProperties, int x, int z)
    {
        if (CarpetSettings.fogOff) return false;
        return skyProperties.isFoggyAt(x, z);
    }

}
