package carpet.mixins;

import carpet.CarpetSettings;
import carpet.fakes.EntityInterface;
import carpet.helpers.BlockRotator;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Direction.class)
public abstract class DirectionMixin
{
    @Redirect(method = "orderedByNearest", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"))
    private static float getYaw(Entity entity, float float_1)
    {
        float yaw;
        if (!CarpetSettings.placementRotationFix)
        {
            yaw = entity.getViewYRot(float_1);
        }
        else
        {
            yaw = ((EntityInterface) entity).getMainYaw(float_1);
        }
        if (BlockRotator.flippinEligibility(entity))
        {
            yaw += 180f;
        }
        return yaw;
    }
    @Redirect(method = "orderedByNearest", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"))
    private static float getPitch(Entity entity, float float_1)
    {
        float pitch = entity.getViewXRot(float_1);
        if (BlockRotator.flippinEligibility(entity))
        {
            pitch = -pitch;
        }
        return pitch;
    }
}
