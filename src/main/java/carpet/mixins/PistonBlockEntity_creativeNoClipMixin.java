package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PistonBlockEntity.class)
public class PistonBlockEntity_creativeNoClipMixin
{
    @Inject(method = "method_23672", at = @At("HEAD"), cancellable = true)
    private static void dontPushSpectators(Direction direction, Entity entity, double d, Direction direction2, CallbackInfo ci)
    {
        if (CarpetSettings.creativeNoClip && entity instanceof PlayerEntity && (((PlayerEntity) entity).isCreative()) && ((PlayerEntity) entity).abilities.flying) ci.cancel();
    }

    @Redirect(method = "pushEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;setVelocity(DDD)V"))
    private void ignoreAccel(Entity entity, double x, double y, double z)
    {
        if (CarpetSettings.creativeNoClip && entity instanceof PlayerEntity && (((PlayerEntity) entity).isCreative()) && ((PlayerEntity) entity).abilities.flying) return;
        entity.setVelocity(x,y,z);
    }
}
