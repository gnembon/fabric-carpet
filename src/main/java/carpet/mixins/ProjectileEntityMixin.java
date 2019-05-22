package carpet.mixins;

import carpet.logging.LoggerRegistry;
import carpet.logging.logHelpers.TrajectoryLogHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ProjectileEntity.class)
public abstract class ProjectileEntityMixin extends Entity
{
    private TrajectoryLogHelper logHelper;
    public ProjectileEntityMixin(EntityType<?> entityType_1, World world_1) { super(entityType_1, world_1); }

    @Inject(method = "<init>(Lnet/minecraft/entity/EntityType;Lnet/minecraft/world/World;)V", at = @At("RETURN"))
    private void addLogger(EntityType<? extends ProjectileEntity> entityType_1, World world_1, CallbackInfo ci)
    {
        if (LoggerRegistry.__projectiles && !world_1.isClient)
            logHelper = new TrajectoryLogHelper("projectiles");
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tickCheck(CallbackInfo ci)
    {
        if (LoggerRegistry.__projectiles && logHelper != null)
            logHelper.onTick(x, y, z, getVelocity());
    }

    @Inject(method = "onHit(Lnet/minecraft/util/hit/HitResult;)V", at = @At("RETURN"))
    private void remove(HitResult hitResult_1, CallbackInfo ci)
    {
        if (LoggerRegistry.__projectiles &&
                (hitResult_1.getType() == HitResult.Type.ENTITY || hitResult_1.getType() == HitResult.Type.BLOCK)
                && logHelper != null)
        {
            logHelper.onFinish();
            logHelper = null;
        }
    }
}
