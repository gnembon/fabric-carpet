package carpet.mixins;

import carpet.CarpetSettings;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PistonMovingBlockEntity.class)
public abstract class PistonMovingBlockEntity_playerHandlingMixin
{

    @Inject(method = "moveEntityByPiston", at = @At("HEAD"), cancellable = true)
    private static void dontPushSpectators(Direction direction, Entity entity, double d, Direction direction2, CallbackInfo ci)
    {
        if (CarpetSettings.creativeNoClip && entity instanceof Player && (((Player) entity).isCreative()) && ((Player) entity).getAbilities().flying) ci.cancel();
    }

    @Redirect(method = "moveCollidedEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setDeltaMovement(DDD)V"))
    private static void ignoreAccel(Entity entity, double x, double y, double z)
    {
        if (CarpetSettings.creativeNoClip && entity instanceof Player && (((Player) entity).isCreative()) && ((Player) entity).getAbilities().flying) return;
        entity.setDeltaMovement(x,y,z);
    }

    @Redirect(method = "moveCollidedEntities", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;getPistonPushReaction()Lnet/minecraft/world/level/material/PushReaction;"
    ))
    private static PushReaction moveFakePlayers(Entity entity,
        Level world, BlockPos blockPos, float ff, PistonMovingBlockEntity pistonBlockEntity)
    {
        if (entity instanceof EntityPlayerMPFake && pistonBlockEntity.getMovedState().is(Blocks.SLIME_BLOCK))
        {
            Vec3 vec3d = entity.getDeltaMovement();
            double e = vec3d.x;
            double f = vec3d.y;
            double g = vec3d.z;
            Direction direction = pistonBlockEntity.getMovementDirection();
            switch(direction.getAxis()) {
                case X:
                    e = direction.getStepX();
                    break;
                case Y:
                    f = direction.getStepY();
                    break;
                case Z:
                    g = direction.getStepZ();
            }

            entity.setDeltaMovement(e, f, g);
        }
        return entity.getPistonPushReaction();
    }

}
