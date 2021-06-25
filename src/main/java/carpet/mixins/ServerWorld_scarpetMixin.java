package carpet.mixins;

import carpet.CarpetSettings;
import carpet.fakes.ServerWorldInterface;
import carpet.script.CarpetEventServer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.damage.DamageSource;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static carpet.script.CarpetEventServer.Event.ENTITY_HANDLER;
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


    private ThreadLocal<Boolean> entityJustLoaded = ThreadLocal.withInitial(() -> false);

    @Inject(method = "loadEntityUnchecked", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerChunkManager;loadEntity(Lnet/minecraft/entity/Entity;)V"
    ))
            private void onEntityAddedToWorld(Entity entity, CallbackInfo ci)
    {
        boolean justLoaded = entityJustLoaded.get();
        entityJustLoaded.set(false);
        CarpetEventServer.Event event = ENTITY_HANDLER.get(entity.getType());
        if (event != null)
        {
            if (event.isNeeded())
            {
                event.onEntityAction(entity, !justLoaded);
            }
        }
        else
        {
            CarpetSettings.LOG.error("Failed to handle entity "+entity.getType().getTranslationKey());
        }

        // deprecated usage
        event = ENTITY_LOAD.get(entity.getType());
        if (event != null)
        {
            if (event.isNeeded())
            {
                event.onEntityAction(entity, true);
            }
        }
        else
        {
            CarpetSettings.LOG.error("Failed to handle entity "+entity.getType().getTranslationKey());
        }
    }

    // 1.16only
    @Inject(method = "loadEntity", at= @At("HEAD"))
    private void onSideLoaded(Entity entity, CallbackInfoReturnable<Boolean> cir)
    {
        entityJustLoaded.set(true);
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
