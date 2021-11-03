package carpet.mixins;

import carpet.helpers.OptimizedExplosion;
import carpet.CarpetSettings;
import carpet.logging.LoggerRegistry;
import carpet.logging.logHelpers.ExplosionLogHelper;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.chunk.LevelChunk;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
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

import java.util.Iterator;

@Mixin(value = Explosion.class)
public abstract class Explosion_optimizedTntMixin
{
    @Shadow
    @Final
    private ObjectArrayList<BlockPos> toBlow;

    @Shadow
    @Final
    private Level level;

    @Unique
    private ExplosionLogHelper eLogger;

    @Unique
    private boolean shouldPlaySoundAndParticles = false;

    @Unique
    private LevelChunk chunkCache = null;

    @Unique
    private BlockPos blockBelowAffected = null;

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

    @Inject(method = "finalizeExplosion", at = @At("HEAD"))
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
    }

    /*
    doExplosionB mixin rework starts
    =====================================================================================
     */

    @Redirect(
            method = "finalizeExplosion",
            at = @At(
                    value = "FIELD",
                    opcode = Opcodes.GETFIELD,
                    target = "Lnet/minecraft/world/level/Level;isClientSide:Z"
            )
    )
    private boolean shouldPlaySound(Level level) {
        if (CarpetSettings.optimizedTNT) {
            shouldPlaySoundAndParticles = OptimizedExplosion.explosionSound < 100 || OptimizedExplosion.explosionSound % 100 == 0;
            return shouldPlaySoundAndParticles;
        }
        return level.isClientSide;
    }

    @Redirect(
            method = "finalizeExplosion",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V"
            )
    )
    private void playSound(Level level, double x, double y, double z, SoundEvent sound, SoundSource source, float volume, float pitch, boolean bl) {
        if (CarpetSettings.optimizedTNT){
            level.playSound(null, x, y, z, sound, source, volume, pitch);
        } else {
            level.playLocalSound(x, y, z, sound, source, volume, pitch, bl);
        }
    }

    @ModifyVariable(
            method = "finalizeExplosion",
            at = @At(
                    value = "LOAD",
                    opcode = Opcodes.ILOAD
            ),
            ordinal = 0
    )
    private boolean shouldPlayParticles(boolean particles) {
        if (CarpetSettings.optimizedTNT){
            return shouldPlaySoundAndParticles;
        }
        return particles;
    }

    @Redirect(
            method = "finalizeExplosion",
            at = @At(
                    value = "INVOKE",
                    target = "java/util/Iterator.next()Ljava/lang/Object;",
                    ordinal = 2
            )
    )
    private <E> E cacheChunkAndBlock(Iterator<E> iterator) {
        E affectedCache = iterator.next();
        if (CarpetSettings.optimizedTNT) {
            blockBelowAffected = ((BlockPos) affectedCache).below();
            chunkCache = level.getChunk(blockBelowAffected.getX() >> 4, blockBelowAffected.getZ() >> 4);
        }
        return affectedCache;
    }

    @Redirect(
            method = "finalizeExplosion",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
                    ordinal = 1
            )
    )
    private BlockState useChunkCache(Level level, BlockPos pos) {
        if (CarpetSettings.optimizedTNT) {
            return chunkCache.getBlockState(pos);
        }
        return level.getBlockState(pos);
    }

    @Redirect(
            method = "finalizeExplosion",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;below()Lnet/minecraft/core/BlockPos;"
            )
    )
    private BlockPos useBlockBelowCache(BlockPos blockPos) {
        if (CarpetSettings.optimizedTNT) {
            return blockBelowAffected;
        }
        return blockPos;
    }

    @Redirect(
            method = "finalizeExplosion",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/BaseFireBlock;getState(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"
            )
    )
    private BlockState useDefaultFireState(BlockGetter level, BlockPos pos) {
        if (CarpetSettings.optimizedTNT) {
            return Blocks.FIRE.defaultBlockState();
        }
        return BaseFireBlock.getState(level, pos);
    }

    /*
    doExplosionB mixin rework ends
    =====================================================================================
     */

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
