package carpet.mixins;

import carpet.fakes.ThreadedAnvilChunkStorageInterface;
import carpet.helpers.TickSpeed;
import carpet.utils.CarpetProfiler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import com.google.common.collect.Lists;

@Mixin(ServerChunkCache.class)
public abstract class ServerChunkCache_tickMixin
{

    @Shadow @Final ServerLevel level;

    @Shadow @Final
    public ChunkMap chunkMap;

    CarpetProfiler.ProfilerToken currentSection;

    @Inject(method = "tickChunks", at = @At("HEAD"))
    private void startSpawningSection(CallbackInfo ci)
    {
        currentSection = CarpetProfiler.start_section(level, "Spawning", CarpetProfiler.TYPE.GENERAL);
    }

    @Inject(method = "tickChunks", at = @At(
            value = "FIELD",
            target = "net/minecraft/server/level/ServerChunkCache.level:Lnet/minecraft/server/level/ServerLevel;",
            ordinal = 10
    ))
    private void skipChunkTicking(CallbackInfo ci)
    {
        if (currentSection != null)
        {
            CarpetProfiler.end_current_section(currentSection);
        }
    }

    @Inject(method = "tickChunks", at = @At(
            value = "INVOKE",
            target = "net/minecraft/server/level/ServerLevel.tickChunk(Lnet/minecraft/world/level/chunk/LevelChunk;I)V",
            shift = At.Shift.AFTER
    ))
    private void resumeSpawningSection(CallbackInfo ci)
    {
        currentSection = CarpetProfiler.start_section(level, "Spawning", CarpetProfiler.TYPE.GENERAL);
    }

    @Inject(method = "tickChunks", at = @At("RETURN"))
    private void stopSpawningSection(CallbackInfo ci)
    {
        if (currentSection != null)
        {
            CarpetProfiler.end_current_section(currentSection);
        }
    }

    //// Tick freeze
    @Redirect(method = "tickChunks", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;isDebug()Z"
    ))
    private boolean skipChunkTicking(ServerLevel serverWorld)
    {
        boolean debug = serverWorld.isDebug();
        if (!TickSpeed.process_entities)
        {
            // simplified chunk tick iteration assuming world is frozen otherwise as suggested by Hadron67
            // to be kept in sync with the original injection source
            if (!debug){
                List<ChunkHolder> holders = Lists.newArrayList(((ThreadedAnvilChunkStorageInterface)chunkMap).getChunksCM());
                Collections.shuffle(holders);
                for (ChunkHolder holder: holders){
                    Optional<LevelChunk> optional = holder.getTickingChunkFuture().getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK).left();
                    if (optional.isPresent()){
                        holder.broadcastChanges(optional.get());
                    }
                }
            }
            return true;
        }
        return debug;
    }

}
