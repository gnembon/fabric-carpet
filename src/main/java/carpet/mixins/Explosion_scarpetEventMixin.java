package carpet.mixins;

import net.minecraft.world.level.ServerExplosion;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static carpet.script.CarpetEventServer.Event.EXPLOSION_OUTCOME;

@Mixin(value = ServerExplosion.class, priority = 990)
public abstract class Explosion_scarpetEventMixin
{
    @Shadow @Final private ServerLevel level;
    @Shadow @Final private Vec3 center;
    @Shadow @Final private float radius;
    @Shadow @Final private boolean fire;
    @Shadow @Final private Explosion.BlockInteraction blockInteraction;
    @Shadow @Final private @Nullable Entity source;

    @Shadow /*@Nullable*/ public abstract /*@Nullable*/ LivingEntity getIndirectSourceEntity();

    private List<Entity> affectedEntities = new ArrayList<>();

    @Inject(method = "explode", at = @At("HEAD"))
    private void explodeCM(CallbackInfo ci)
    {
        affectedEntities.clear();
    }

    @Redirect(method = "hurtEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;onExplosionHit(Lnet/minecraft/world/entity/Entity;)V"))
    private void onEntityHit(Entity instance, Entity entity)
    {
        affectedEntities.add(instance);
        instance.onExplosionHit(entity);
    }

    @Inject(method = "explode", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ServerExplosion;hurtEntities()V", shift = At.Shift.AFTER))
    private void onExplosionDone(CallbackInfo ci, List list)
    {
        if (EXPLOSION_OUTCOME.isNeeded() && !level.isClientSide())
        {
            EXPLOSION_OUTCOME.onExplosion((ServerLevel) level, source, this::getIndirectSourceEntity, center, radius, fire, list, affectedEntities, blockInteraction);
        }
    }
}
