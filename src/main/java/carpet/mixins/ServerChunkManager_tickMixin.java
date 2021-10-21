package carpet.mixins;

import carpet.fakes.ThreadedAnvilChunkStorageInterface;
import carpet.helpers.TickSpeed;
import carpet.utils.CarpetProfiler;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.world.chunk.WorldChunk;

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

import com.google.common.collect.Lists;

@Mixin(ServerChunkManager.class)
public abstract class ServerChunkManager_tickMixin
{

    @Shadow @Final private ServerWorld world;

    @Shadow @Final
    public ThreadedAnvilChunkStorage threadedAnvilChunkStorage;

    CarpetProfiler.ProfilerToken currentSection;

    @Inject(method = "tickChunks", at = @At("HEAD"))
    private void startSpawningSection(CallbackInfo ci)
    {
        currentSection = CarpetProfiler.start_section(world, "Spawning and Random Ticks", CarpetProfiler.TYPE.GENERAL);
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
            target = "Lnet/minecraft/server/world/ServerWorld;isDebugWorld()Z"
    ))
    private boolean skipChunkTicking(ServerWorld serverWorld)
    {
        boolean debug = serverWorld.isDebugWorld();
        if (!TickSpeed.process_entities)
        {
            // simplified chunk tick iteration assuming world is frozen otherwise as suggested by Hadron67
            // to be kept in sync with the original injection source
            if (!debug){
                List<ChunkHolder> holders = Lists.newArrayList(((ThreadedAnvilChunkStorageInterface)threadedAnvilChunkStorage).getChunksCM());
                Collections.shuffle(holders);
                for (ChunkHolder holder: holders){
                    Optional<WorldChunk> optional = holder.getTickingFuture().getNow(ChunkHolder.UNLOADED_WORLD_CHUNK).left();
                    if (optional.isPresent()){
                        holder.flushUpdates(optional.get());
                    }
                }
            }
            return true;
        }
        return debug;
    }

}
