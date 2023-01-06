package carpet.mixins;

import carpet.logging.LoggerRegistry;
import carpet.logging.logHelpers.TrajectoryLogHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityMixin extends Entity
{
    private TrajectoryLogHelper logHelper;
    public FallingBlockEntityMixin(EntityType<?> entityType_1, Level world_1) { super(entityType_1, world_1); }

    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("RETURN"))
    private void addLogger(EntityType<? extends Projectile> entityType_1, Level world_1, CallbackInfo ci)
    {
        if (LoggerRegistry.__fallingBlocks && !world_1.isClientSide)
            logHelper = new TrajectoryLogHelper("fallingBlocks");
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tickCheck(CallbackInfo ci)
    {
        if (LoggerRegistry.__fallingBlocks && logHelper != null)
            logHelper.onTick(getX(), getY(), getZ(), getDeltaMovement());
    }

    @Override
    public void remove(Entity.RemovalReason arg) // reason
    {
        super.remove(arg);
        if (LoggerRegistry.__fallingBlocks && logHelper != null)
            logHelper.onFinish();
    }
}
