package carpet.mixins;

import carpet.CarpetSettings;
import carpet.fakes.ServerWorldInterface;
import carpet.script.CarpetEventServer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.level.ServerWorldProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static carpet.script.CarpetEventServer.Event.ENTITY_LOAD;
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

    @Inject(method = "loadEntityUnchecked", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerChunkManager;loadEntity(Lnet/minecraft/entity/Entity;)V"
    ))
            private void onEntityAddedToWorld(Entity entity, CallbackInfo ci)
    {
        CarpetEventServer.Event event = ENTITY_LOAD.get(entity.getType());
        if (event != null)
        {
            if (event.isNeeded())
            {
                event.onEntityAction(entity);
            }
        }
        else
        {
            CarpetSettings.LOG.error("Failed to handle entity "+entity.getType().getTranslationKey());
        }
    }

    @Shadow
    private ServerWorldProperties worldProperties;
    public ServerWorldProperties getWorldPropertiesCM(){
        return worldProperties;
    }
}
