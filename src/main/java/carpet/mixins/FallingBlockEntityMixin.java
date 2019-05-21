package carpet.mixins;

import carpet.logging.LoggerRegistry;
import carpet.logging.logHelpers.TrajectoryLogHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityMixin extends Entity
{
    private TrajectoryLogHelper logHelper;
    public FallingBlockEntityMixin(EntityType<?> entityType_1, World world_1) { super(entityType_1, world_1); }

    @Inject(method = "<init>(Lnet/minecraft/entity/EntityType;Lnet/minecraft/world/World;)V", at = @At("RETURN"))
    private void addLogger(EntityType<? extends ProjectileEntity> entityType_1, World world_1, CallbackInfo ci)
    {
        if (LoggerRegistry.__fallingBlocks && !world_1.isClient)
            logHelper = new TrajectoryLogHelper("fallingBlocks");
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tickCheck(CallbackInfo ci)
    {
        if (LoggerRegistry.__fallingBlocks && logHelper != null)
            logHelper.onTick(x, y, z, getVelocity());
    }

    @Override
    public void remove()
    {
        super.remove();
        if (LoggerRegistry.__fallingBlocks && logHelper != null)
            logHelper.onFinish();
    }
}
