package carpet.mixins;

import carpet.CarpetSettings;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
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

    @Inject(method = "moveEntity", at = @At("HEAD"), cancellable = true)
    private static void dontPushSpectators(Direction direction, Entity entity, double d, Direction direction2, CallbackInfo ci)
    {
        if (CarpetSettings.creativeNoClip && entity instanceof PlayerEntity && (((PlayerEntity) entity).isCreative()) && ((PlayerEntity) entity).getAbilities().flying) ci.cancel();
    }

    @Redirect(method = "pushEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;setVelocity(DDD)V"))
    private static void ignoreAccel(Entity entity, double x, double y, double z)
    {
        if (CarpetSettings.creativeNoClip && entity instanceof PlayerEntity && (((PlayerEntity) entity).isCreative()) && ((PlayerEntity) entity).getAbilities().flying) return;
        entity.setVelocity(x,y,z);
    }

    @Redirect(method = "pushEntities", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;getPistonBehavior()Lnet/minecraft/block/piston/PistonBehavior;"
    ))
    private static PistonBehavior moveFakePlayers(Entity entity,
        World world, BlockPos blockPos, float ff, PistonBlockEntity pistonBlockEntity)
    {
        if (entity instanceof EntityPlayerMPFake && pistonBlockEntity.getPushedBlock().isOf(Blocks.SLIME_BLOCK))
        {
            Vec3d vec3d = entity.getVelocity();
            double e = vec3d.x;
            double f = vec3d.y;
            double g = vec3d.z;
            Direction direction = pistonBlockEntity.getMovementDirection();
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
