package carpet.mixins;

import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WorldRenderer.class)
public class WorldRenderer_pausedShakeMixin
{
    /* todo - figure out proper injections to render entity
    @Redirect(method = "method_22710", @At(
            value = "INVOKE",
            target = "float_1"
    ))
    */

}
