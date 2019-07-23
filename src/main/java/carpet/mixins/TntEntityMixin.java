package carpet.mixins;

import carpet.settings.CarpetSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import carpet.logging.LoggerRegistry;
import carpet.logging.logHelpers.TNTLogHelper;

@Mixin(TntEntity.class)
public abstract class TntEntityMixin extends Entity
{
    private TNTLogHelper logHelper;

    public TntEntityMixin(EntityType<?> entityType_1, World world_1)
    {
        super(entityType_1, world_1);
    }

    @Inject(method = "<init>(Lnet/minecraft/entity/EntityType;Lnet/minecraft/world/World;)V", at = @At("RETURN"))
    private void initTNTLoggerPrime(EntityType<? extends TntEntity> entityType_1, World world_1, CallbackInfo ci)
    {
        if (LoggerRegistry.__tnt && !world_1.isClient)
        {
            logHelper = new TNTLogHelper();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void initTracker(CallbackInfo ci)
    {
        if (LoggerRegistry.__tnt && logHelper != null && !logHelper.initialized)
        {
            logHelper.onPrimed(x, y, z, getVelocity());
        }
    }


    @Inject(method = "<init>(Lnet/minecraft/world/World;DDDLnet/minecraft/entity/LivingEntity;)V",
            at = @At(value = "RETURN"))
    private void initTNTLogger(World world_1, double double_1, double double_2, double double_3,
                               LivingEntity livingEntity_1, CallbackInfo ci)
    {
        if(CarpetSettings.tntPrimerMomentumRemoved)
            this.setVelocity(new Vec3d(0.0, 0.20000000298023224D, 0.0));
    }

    @Inject(method = "explode", at = @At(value = "HEAD"))
    private void onExplode(CallbackInfo ci)
    {
        if (LoggerRegistry.__tnt && logHelper != null)
            logHelper.onExploded(x, y, z);
    }
}