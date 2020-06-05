package carpet.mixins;

import carpet.helpers.OptimizedExplosion;
import carpet.CarpetSettings;
import carpet.logging.LoggerRegistry;
import carpet.logging.logHelpers.ExplosionLogHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = Explosion.class)
public abstract class ExplosionMixin
{
    @Shadow
    @Final
    private List<BlockPos> affectedBlocks;

    @Shadow @Final private World world;

    private ExplosionLogHelper eLogger;

    @Inject(method = "collectBlocksAndDamageEntities", at = @At("HEAD"),
            cancellable = true)
    private void onExplosionA(CallbackInfo ci)
    {
        if (CarpetSettings.optimizedTNT)
        {
            OptimizedExplosion.doExplosionA((Explosion) (Object) this, eLogger);
            ci.cancel();
        }
    }

    @Inject(method = "affectWorld", at = @At("HEAD"),
            cancellable = true)
    private void onExplosionB(boolean spawnParticles, CallbackInfo ci)
    {
        if (eLogger != null)
        {
            eLogger.setAffectBlocks( ! affectedBlocks.isEmpty());
            eLogger.onExplosionDone(this.world.getTime());
        }
        if (CarpetSettings.explosionNoBlockDamage)
        {
            affectedBlocks.clear();
        }
        if (CarpetSettings.optimizedTNT)
        {
            OptimizedExplosion.doExplosionB((Explosion) (Object) this, spawnParticles);
            ci.cancel();
        }
    }
    //optional due to Overwrite in Lithium
    //should kill most checks if no block damage is requested
    @Redirect(method = "collectBlocksAndDamageEntities", require = 0, at = @At(value = "INVOKE",
            target ="Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;"))
    private BlockState noBlockCalcsWithNoBLockDamage(World world, BlockPos pos)
    {
        if (CarpetSettings.explosionNoBlockDamage) return Blocks.BEDROCK.getDefaultState();
        return world.getBlockState(pos);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;DDDFZLnet/minecraft/world/explosion/Explosion$DestructionType;)V",
            at = @At(value = "RETURN"))
    private void onExplosionCreated(World world, Entity entity, double x, double y, double z, float power, boolean createFire, Explosion.DestructionType blockDestructionType, CallbackInfo ci)
    {
        if (LoggerRegistry.__explosions && ! world.isClient)
        {
            eLogger = new ExplosionLogHelper(entity, x, y, z, power, createFire, blockDestructionType);
        }
    }

    @Redirect(method = "collectBlocksAndDamageEntities",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V"))
    private void setVelocityAndUpdateLogging(Entity entity, Vec3d velocity)
    {
        if (eLogger != null) {
            eLogger.onEntityImpacted(entity, velocity.subtract(entity.getVelocity()));
        }
        entity.setVelocity(velocity);
    }
}
