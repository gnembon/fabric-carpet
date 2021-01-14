package carpet.mixins;

import carpet.logging.LoggerRegistry;
import carpet.logging.logHelpers.ExplosionLogHelper;
import carpet.script.value.EntityValue;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static carpet.script.CarpetEventServer.Event.EXPLOSION;

@Mixin(value = Explosion.class, priority = 990)
public abstract class Explosion_scarpetEventMixin
{
    @Shadow @Final private World world;
    @Shadow @Final private double x;
    @Shadow @Final private double y;
    @Shadow @Final private double z;
    @Shadow @Final private float power;
    @Shadow @Final private DamageSource damageSource;
    @Shadow @Final private boolean createFire;
    @Shadow @Final private List<BlockPos> affectedBlocks;
    private List<Entity> affectedEntities;

    @Inject(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/damage/DamageSource;Lnet/minecraft/world/explosion/ExplosionBehavior;DDDFZLnet/minecraft/world/explosion/Explosion$DestructionType;)V",
            at = @At(value = "RETURN"))
    private void onExplosionCreated(World world, Entity entity, DamageSource damageSource, ExplosionBehavior explosionBehavior, double x, double y, double z, float power, boolean createFire, Explosion.DestructionType destructionType, CallbackInfo ci)
    {
        if (EXPLOSION.isNeeded())
        {
            affectedEntities = new ArrayList<>();
        }
    }

    @Inject(method = "collectBlocksAndDamageEntities", locals= LocalCapture.CAPTURE_FAILHARD, at=@At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"))
    private void onExplosion(CallbackInfo ci, Set<BlockPos> s1, float f1, int i1, int i2, int i3, int i4, int i5, int i6, List<Entity> l1, Vec3d v1, int i7, Entity entity)
    {
        if (EXPLOSION.isNeeded())
        {
            affectedEntities.add(entity);
        }
    }

    @Inject(method = "affectWorld", at = @At("HEAD"))
    private void onExplosion(boolean spawnParticles, CallbackInfo ci)
    {
        if (EXPLOSION.isNeeded())
        {
            EXPLOSION.onExplosion((ServerWorld) world, x, y, z, power, damageSource, createFire, affectedBlocks, affectedEntities);
        }
    }
}
