package carpet.mixins;

import carpet.logging.LoggerRegistry;
import carpet.logging.logHelpers.TrajectoryLogHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin extends Entity
{
    private TrajectoryLogHelper logHelper;
    public AbstractArrowMixin(EntityType<?> entityType_1, Level world_1) { super(entityType_1, world_1); }

    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("RETURN"))
    private void addLogger(EntityType<? extends Projectile> entityType_1, Level world_1, CallbackInfo ci)
    {
        if (LoggerRegistry.__projectiles && !world_1.isClientSide)
            logHelper = new TrajectoryLogHelper("projectiles");
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tickCheck(CallbackInfo ci)
    {
        if (LoggerRegistry.__projectiles && logHelper != null)
            logHelper.onTick(getX(), getY(), getZ(), getDeltaMovement());
    }

    // todo should be moved on one place this is acceessed from
    @Inject(method = "onHitEntity", at = @At("RETURN"))
    private void removeOnEntity(EntityHitResult entityHitResult, CallbackInfo ci)
    {
        if (LoggerRegistry.__projectiles && logHelper != null)
        {
            logHelper.onFinish();
            logHelper = null;
        }
    }

    @Inject(method = "onHitBlock", at = @At("RETURN"))
    private void removeOnBlock(BlockHitResult blockHitResult, CallbackInfo ci)
    {
        if (LoggerRegistry.__projectiles && logHelper != null)
        {
            logHelper.onFinish();
            logHelper = null;
        }
    }
}
