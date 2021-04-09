package carpet.mixins;

import carpet.CarpetSettings;
import carpet.fakes.ServerWorldInterface;
import carpet.script.CarpetEventServer;
import net.minecraft.entity.Entity;
import net.minecraft.world.entity.EntityLike;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerEntityManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;
import net.minecraft.world.level.ServerWorldProperties;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static carpet.script.CarpetEventServer.Event.ENTITY_LOAD;
import static carpet.script.CarpetEventServer.Event.EXPLOSION;
import static carpet.script.CarpetEventServer.Event.LIGHTNING;

@Mixin(ServerWorld.class)
public class ServerWorld_scarpetMixin implements ServerWorldInterface
{
    @Inject(method = "tickChunk", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerWorld;spawnEntity(Lnet/minecraft/entity/Entity;)Z",
            shift = At.Shift.BEFORE,
            ordinal = 1
    ))
    private void onNaturalLightinig(WorldChunk chunk, int randomTickSpeed, CallbackInfo ci,
                                    //ChunkPos chunkPos, boolean bl, int i, int j, Profiler profiler, BlockPos blockPos, boolean bl2)
                                    ChunkPos chunkPos, boolean bl, int i, int j, Profiler profiler, BlockPos blockPos, boolean bl2, LightningEntity lightningEntity)
    {
        if (LIGHTNING.isNeeded()) LIGHTNING.onWorldEventFlag((ServerWorld) (Object)this, blockPos, bl2?1:0);
    }

    @Redirect(method = "addEntity", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerEntityManager;addEntity(Lnet/minecraft/world/entity/EntityLike;)Z"
    ))
    private boolean onEntityAddedToWorld(ServerEntityManager serverEntityManager, EntityLike entityLike)
    {
        Entity entity = (Entity)entityLike;
        boolean success = serverEntityManager.addEntity(entity);
        if (success) {
            CarpetEventServer.Event event = ENTITY_LOAD.get(entity.getType());
            if (event != null) {
                if (event.isNeeded()) {
                    event.onEntityAction(entity);
                }
            } else {
                CarpetSettings.LOG.error("Failed to handle entity " + entity.getType().getTranslationKey());
            }
        };
        return success;
    }

    @Inject(method = "createExplosion", at = @At("HEAD"))
    private void handleExplosion(/*@Nullable*/ Entity entity, /*@Nullable*/ DamageSource damageSource, /*@Nullable*/ ExplosionBehavior explosionBehavior, double d, double e, double f, float g, boolean bl, Explosion.DestructionType destructionType, CallbackInfoReturnable<Explosion> cir)
    {
        if (EXPLOSION.isNeeded())
            EXPLOSION.onExplosion((ServerWorld) (Object)this, entity, null, d, e, f, g, bl, null, null, destructionType);
    }

    @Final
    @Shadow
    private ServerWorldProperties worldProperties;
    public ServerWorldProperties getWorldPropertiesCM(){
        return worldProperties;
    }
}
