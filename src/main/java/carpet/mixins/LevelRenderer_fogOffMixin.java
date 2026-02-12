package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = AtmosphericFogEnvironment.class, priority = 69420)
public class LevelRenderer_fogOffMixin
{
    @Redirect(method = "setupFog", require = 0, expect = 0, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/attribute/EnvironmentAttributeProbe;getValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;F)Ljava/lang/Object;"
    ))
    private <Value> Value isReallyThick(EnvironmentAttributeProbe instance, EnvironmentAttribute<Value> environmentAttribute, float f)
    {
        if (CarpetSettings.fogOff) return environmentAttribute.defaultValue();
        return instance.getValue(environmentAttribute, f);
    }

}
