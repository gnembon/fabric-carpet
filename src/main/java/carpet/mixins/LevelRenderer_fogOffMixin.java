package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.client.renderer.GameRenderer;
//import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = GameRenderer.class, priority = 69420)
public class LevelRenderer_fogOffMixin
{
    @Redirect(method = "renderLevel", require = 0, expect = 0, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/attribute/EnvironmentAttributeProbe;getValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;F)Ljava/lang/Object;"
    ))
    private <Value> Value isReallyThick(EnvironmentAttributeProbe instance, EnvironmentAttribute<Value> environmentAttribute, float f)
    {
        if (CarpetSettings.fogOff) return (Value) Boolean.FALSE;
        return instance.getValue(environmentAttribute, f);
    }

}
