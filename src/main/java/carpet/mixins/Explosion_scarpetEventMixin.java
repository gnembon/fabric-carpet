package carpet.mixins;

import org.spongepowered.asm.mixin.injection.ModifyArg;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;

import static carpet.script.CarpetEventServer.Event.EXPLOSION_OUTCOME;

@Mixin(value = Explosion.class, priority = 990)
public abstract class Explosion_scarpetEventMixin
{
    @Shadow @Final private Level level;
    @Shadow @Final private double x;
    @Shadow @Final private double y;
    @Shadow @Final private double z;
    @Shadow @Final private float radius;
    @Shadow @Final private boolean fire;
    @Shadow @Final private ObjectArrayList<BlockPos> toBlow;
    @Shadow @Final private Explosion.BlockInteraction blockInteraction;
    @Shadow @Final private @Nullable Entity source;

    @Shadow /*@Nullable*/ public abstract /*@Nullable*/ LivingEntity getIndirectSourceEntity();

    private List<Entity> affectedEntities;

    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Explosion$BlockInteraction;Lnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/core/Holder;)V",
            at = @At(value = "RETURN"))
    private void onExplosionCreated(Level world, Entity entity, DamageSource damageSource, ExplosionDamageCalculator explosionBehavior, double x, double y, double z, float power, boolean createFire, Explosion.BlockInteraction destructionType, ParticleOptions particleOptions, ParticleOptions particleOptions2, Holder soundEvent, CallbackInfo ci)
    {
        if (EXPLOSION_OUTCOME.isNeeded() && !world.isClientSide())
        {
            affectedEntities = new ArrayList<>();
        }
    }

    @ModifyArg(method = "explode", at=@At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Explosion;getSeenPercent(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/entity/Entity;)F")
    )
    private Entity onExplosion(Entity entity)
    {
        if (affectedEntities != null)
        {
            affectedEntities.add(entity);
        }
        return entity;
    }

    @Inject(method = "finalizeExplosion", at = @At("HEAD"))
    private void onExplosion(boolean spawnParticles, CallbackInfo ci)
    {
        if (EXPLOSION_OUTCOME.isNeeded() && !level.isClientSide())
        {
            EXPLOSION_OUTCOME.onExplosion((ServerLevel) level, source, this::getIndirectSourceEntity, x, y, z, radius, fire, toBlow, affectedEntities, blockInteraction);
        }
    }
}
