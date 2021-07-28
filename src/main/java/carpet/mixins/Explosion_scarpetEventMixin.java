package carpet.mixins;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static carpet.script.CarpetEventServer.Event.EXPLOSION_OUTCOME;

@Mixin(value = Explosion.class, priority = 990)
public abstract class Explosion_scarpetEventMixin
{
    @Shadow @Final private World world;
    @Shadow @Final private double x;
    @Shadow @Final private double y;
    @Shadow @Final private double z;
    @Shadow @Final private float power;
    @Shadow @Final private boolean createFire;
    @Shadow @Final private List<BlockPos> affectedBlocks;
    @Shadow @Final private Explosion.DestructionType destructionType;
    @Shadow @Final private @Nullable Entity entity;

    @Shadow /*@Nullable*/ public abstract /*@Nullable*/ LivingEntity getCausingEntity();

    @Shadow public static float getExposure(Vec3d source, Entity entity) {return 0.0f;}

    private List<Entity> affectedEntities;

    @Inject(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/damage/DamageSource;Lnet/minecraft/world/explosion/ExplosionBehavior;DDDFZLnet/minecraft/world/explosion/Explosion$DestructionType;)V",
            at = @At(value = "RETURN"))
    private void onExplosionCreated(World world, Entity entity, DamageSource damageSource, ExplosionBehavior explosionBehavior, double x, double y, double z, float power, boolean createFire, Explosion.DestructionType destructionType, CallbackInfo ci)
    {
        if (EXPLOSION_OUTCOME.isNeeded() && !world.isClient())
        {
            affectedEntities = new ArrayList<>();
        }
    }

    @Redirect(method = "collectBlocksAndDamageEntities", at=@At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/explosion/Explosion;getExposure(Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/entity/Entity;)F")
    )
    private float onExplosion(Vec3d source, Entity entity)
    {
        if (affectedEntities != null)
        {
            affectedEntities.add(entity);
        }
        return getExposure(source, entity);
    }

    @Inject(method = "affectWorld", at = @At("HEAD"))
    private void onExplosion(boolean spawnParticles, CallbackInfo ci)
    {
        if (EXPLOSION_OUTCOME.isNeeded() && !world.isClient())
        {
            EXPLOSION_OUTCOME.onExplosion((ServerWorld) world, entity, this::getCausingEntity, x, y, z, power, createFire, affectedBlocks, affectedEntities, destructionType);
        }
    }
}
