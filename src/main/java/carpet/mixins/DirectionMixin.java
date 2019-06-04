package carpet.mixins;

import carpet.CarpetSettings;
import carpet.fakes.EntityInterface;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Direction.class)
public abstract class DirectionMixin
{
    @Redirect(method = "getEntityFacingOrder", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getYaw(F)F"))
    private static float ifGetYaw(Entity entity, float float_1)
    {
        if (!CarpetSettings.getBool("placementRotationFix"))
        {
            return entity.getYaw(1.0F);
        }
        else
        {
            return ((EntityInterface) entity).getMainYaw(1.0F);
        }
    }
}
