package carpet.mixins;

import carpet.helpers.OptimizedExplosion;
import carpet.CarpetSettings;
import carpet.logging.LoggerRegistry;
import carpet.logging.logHelpers.ExplosionLogHelper;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

@Mixin(value = Explosion.class)
public abstract class Explosion_optimizedTntMixin
{
    @Shadow
    @Final
    private ObjectArrayList<BlockPos> toBlow;

    @Shadow @Final private Level level;

    private ExplosionLogHelper eLogger;

    @Inject(method = "explode", at = @At("HEAD"),
            cancellable = true)
    private void onExplosionA(CallbackInfo ci)
    {
        if (CarpetSettings.optimizedTNT)
        {
            OptimizedExplosion.doExplosionA((Explosion) (Object) this, eLogger);
            ci.cancel();
        }
    }

    @Inject(method = "finalizeExplosion", at = @At("HEAD"),
            cancellable = true)
    private void onExplosionB(boolean spawnParticles, CallbackInfo ci)
    {
        if (eLogger != null)
        {
            eLogger.setAffectBlocks( ! toBlow.isEmpty());
            eLogger.onExplosionDone(this.level.getGameTime());
        }
        if (CarpetSettings.explosionNoBlockDamage)
        {
            toBlow.clear();
        }
        if (CarpetSettings.optimizedTNT)
        {
            OptimizedExplosion.doExplosionB((Explosion) (Object) this, spawnParticles);
            ci.cancel();
        }
    }
    //optional due to Overwrite in Lithium
    //should kill most checks if no block damage is requested
    @Redirect(method = "explode", require = 0, at = @At(value = "INVOKE",
            target ="Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState noBlockCalcsWithNoBLockDamage(Level world, BlockPos pos)
    {
        if (CarpetSettings.explosionNoBlockDamage) return Blocks.BEDROCK.defaultBlockState();
        return world.getBlockState(pos);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Explosion$BlockInteraction;)V",
            at = @At(value = "RETURN"))
    private void onExplosionCreated(Level world, Entity entity, DamageSource damageSource, ExplosionDamageCalculator explosionBehavior, double x, double y, double z, float power, boolean createFire, Explosion.BlockInteraction destructionType, CallbackInfo ci)
    {
        if (LoggerRegistry.__explosions && ! world.isClientSide)
        {
            eLogger = new ExplosionLogHelper(x, y, z, power, createFire, destructionType);
        }
    }

    @Redirect(method = "explode",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    private void setVelocityAndUpdateLogging(Entity entity, Vec3 velocity)
    {
        if (eLogger != null) {
            eLogger.onEntityImpacted(entity, velocity.subtract(entity.getDeltaMovement()));
        }
        entity.setDeltaMovement(velocity);
    }
}
