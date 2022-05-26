package carpet.mixins;

import carpet.fakes.ServerWorldInterface;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static carpet.script.CarpetEventServer.Event.EXPLOSION;
import static carpet.script.CarpetEventServer.Event.LIGHTNING;
import static carpet.script.CarpetEventServer.Event.CHUNK_UNLOADED;


@Mixin(ServerLevel.class)
public class ServerLevel_scarpetMixin implements ServerWorldInterface
{
    @Inject(method = "tickChunk", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z",
            shift = At.Shift.BEFORE,
            ordinal = 1
    ))
    private void onNaturalLightinig(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci,
                                    //ChunkPos chunkPos, boolean bl, int i, int j, Profiler profiler, BlockPos blockPos, boolean bl2)
                                    ChunkPos chunkPos, boolean bl, int i, int j, ProfilerFiller profiler, BlockPos blockPos, DifficultyInstance localDifficulty, boolean bl2, LightningBolt lightningEntity)
    {
        if (LIGHTNING.isNeeded()) LIGHTNING.onWorldEventFlag((ServerLevel) (Object)this, blockPos, bl2?1:0);
    }

    @Inject(method = "explode", at = @At("HEAD"))
    private void handleExplosion(/*@Nullable*/ Entity entity, /*@Nullable*/ DamageSource damageSource, /*@Nullable*/ ExplosionDamageCalculator explosionBehavior, double d, double e, double f, float g, boolean bl, Explosion.BlockInteraction destructionType, CallbackInfoReturnable<Explosion> cir)
    {
        if (EXPLOSION.isNeeded())
            EXPLOSION.onExplosion((ServerLevel) (Object)this, entity, null, d, e, f, g, bl, null, null, destructionType);
    }

    @Inject(method = "unload", at = @At("HEAD"))
    private void handleChunkUnload(LevelChunk levelChunk, CallbackInfo ci)
    {
        if (CHUNK_UNLOADED.isNeeded())
        {
            ServerLevel level = (ServerLevel)((Object)this);
            CHUNK_UNLOADED.onChunkEvent(level, levelChunk.getPos(), false);
        }
    }

    @Final
    @Shadow
    private ServerLevelData serverLevelData;
    @Shadow @Final private PersistentEntitySectionManager<Entity> entityManager;

    public ServerLevelData getWorldPropertiesCM(){
        return serverLevelData;
    }

    @Override
    public LevelEntityGetter<Entity> getEntityLookupCMPublic() {
        return entityManager.getEntityGetter();
    }
}
