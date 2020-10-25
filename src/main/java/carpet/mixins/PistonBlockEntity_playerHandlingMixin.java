package carpet.mixins;

import carpet.CarpetSettings;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PistonBlockEntity.class)
public abstract class PistonBlockEntity_playerHandlingMixin
{
    @Shadow private BlockState pushedBlock;

    @Shadow public abstract Direction getMovementDirection();

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

    @Redirect(method = "pushEntities", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;getPistonBehavior()Lnet/minecraft/block/piston/PistonBehavior;"
    ))
    private PistonBehavior moveFakePlayers(Entity entity)
    {
        if (entity instanceof EntityPlayerMPFake && pushedBlock.isOf(Blocks.SLIME_BLOCK))
        {
            Vec3d vec3d = entity.getVelocity();
            double e = vec3d.x;
            double f = vec3d.y;
            double g = vec3d.z;
            Direction direction = getMovementDirection();
            switch(direction.getAxis()) {
                case X:
                    e = (double)direction.getOffsetX();
                    break;
                case Y:
                    f = (double)direction.getOffsetY();
                    break;
                case Z:
                    g = (double)direction.getOffsetZ();
            }

            entity.setVelocity(e, f, g);
        }
        return entity.getPistonBehavior();
    }

}
