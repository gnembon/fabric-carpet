package carpet.mixins;

import carpet.CarpetSettings;
import carpet.fakes.ChunkHolderInterface;
import carpet.fakes.ThreadedAnvilChunkStorageInterface;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ChunkTaskPrioritySystem;
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.thread.MessageListener;
import net.minecraft.util.thread.ThreadExecutor;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static carpet.script.CarpetEventServer.Event.CHUNK_GENERATED;

@Mixin(ThreadedAnvilChunkStorage.class)
public abstract class ThreadedAnvilChunkStorage_scarpetChunkCreationMixin implements ThreadedAnvilChunkStorageInterface
{
    @Shadow @Final private ServerWorld world;

    @Shadow @Final private LongSet loadedChunks;

    @Shadow @Final private LongSet unloadedChunks;

    @Shadow @Final private Long2ObjectLinkedOpenHashMap<ChunkHolder> currentChunkHolders;

    @Shadow @Final private Long2ObjectLinkedOpenHashMap<ChunkHolder> field_18807;

    @Shadow private boolean chunkHolderListDirty;

    @Shadow protected abstract void method_20458(long l, ChunkHolder chunkHolder);

    @Shadow @Final private Queue<Runnable> field_19343;

    @Shadow @Final private ServerLightingProvider serverLightingProvider;

    @Shadow @Final private ChunkTaskPrioritySystem chunkTaskPrioritySystem;

    @Shadow protected abstract CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> convertToFullChunk(ChunkHolder chunkHolder);

    @Shadow public abstract CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> createChunkFuture(ChunkHolder chunkHolder, ChunkStatus chunkStatus);

    @Shadow @Final private ThreadExecutor<Runnable> mainThreadExecutor;

    @Shadow protected abstract boolean save(Chunk chunk);

    @Shadow @Final private WorldGenerationProgressListener worldGenerationProgressListener;

    @Shadow @Final private MessageListener<ChunkTaskPrioritySystem.Task<Runnable>> worldgenExecutor;

    //in method_20617
    //method_19534(Lnet/minecraft/server/world/ChunkHolder;Lnet/minecraft/world/chunk/Chunk;)Ljava/util/concurrent/CompletableFuture;
    @Inject(method = "method_19534", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ThreadedAnvilChunkStorage;convertToFullChunk(Lnet/minecraft/server/world/ChunkHolder;)Ljava/util/concurrent/CompletableFuture;",
            shift = At.Shift.AFTER
    ))
    private void onChunkGenerated(ChunkHolder chunkHolder, Chunk chunk, CallbackInfoReturnable<CompletableFuture> cir)
    {
        if (CHUNK_GENERATED.isNeeded())
            world.getServer().execute( () -> CHUNK_GENERATED.onChunkGenerated(world, chunk) );
    }

    @Override
    public void regenerateChunkRegion(ChunkPos from, ChunkPos to)
    {
        List<ChunkPos> chunksToConvert = new ArrayList<>();
        int xmax = Math.max(from.x, to.x);
        int zmax = Math.max(from.z, to.z);
        for (int x = Math.min(from.x, to.x); x <= xmax; x++) for (int z = Math.min(from.z, to.z); z <= zmax; z++)
        {
            chunksToConvert.add(new ChunkPos(x,z));
        }

        //flushning all tasks on the thread executor so all futures are ready - we don't want any deadlocks
        mainThreadExecutor.runTasks(() -> mainThreadExecutor.getTaskCount() == 0);
        // save all pending chunks
        // hopefully that would flush all chunk writes, including current chunk, if exists
        Runnable runnable;
        while((runnable = field_19343.poll()) != null) {
            runnable.run();
        }

        List<ChunkHolder> createdChunks = new ArrayList<>();
        for (ChunkPos chpos : chunksToConvert)
        {
            Long id = chpos.toLong();
            //chunk is currently in memory in some shape or form / loaded / unloaded / queued / cached
            ChunkHolder chunkHolder = currentChunkHolders.remove(id);
            if (chunkHolder != null)
            {
                //method_20458(id, chunkHolder); // saving chunk // skipping entities etc
                Chunk chunk = null;
                try
                {
                    // fingers crossed
                    chunk = chunkHolder.getFuture().get();
                }
                catch (InterruptedException | ExecutionException e)
                {
                }
                // if (field_18807.remove(id, chunkHolder) && chunk != null) {
                if (chunk != null)
                {
                    if (chunk instanceof WorldChunk)
                    {
                        ((WorldChunk) chunk).setLoadedToWorld(false);
                    }
                    save(chunk);
                    if (this.loadedChunks.remove(id) && chunk instanceof WorldChunk)
                    {
                        WorldChunk worldChunk = (WorldChunk) chunk;
                        //apparently that doesn't remove them for the client
                        this.world.unloadEntities(worldChunk);
                    }
                    //this.serverLightingProvider.updateChunkStatus(chunk.getPos()); // we might need that as some point
                    serverLightingProvider.tick();
                    worldGenerationProgressListener.setChunkStatus(chunk.getPos(), (ChunkStatus) null);
                }
            }
            unloadedChunks.remove(id);
            loadedChunks.remove(id);
            field_18807.remove(id);
            ChunkHolder newHolder = new ChunkHolder(chpos, 0,serverLightingProvider, chunkTaskPrioritySystem, (ChunkHolder.PlayersWatchingChunkProvider) this);
            this.currentChunkHolders.put(id, newHolder);
            //removed holders
            field_18807.put(id, newHolder);
            //loadedChunks.add(id);
            ((ChunkHolderInterface)newHolder).setDefaultProtoChunk(chpos, mainThreadExecutor);
            createdChunks.add(newHolder);
        }
        //getting rid of all signs
        this.chunkHolderListDirty = true;
        // not needed right now
        for (ChunkHolder newHolder: createdChunks)
        {
            //convert requires a chunk to be unloaded to load entities
            //loadedChunks.remove(newHolder.getPos().toLong());
            //convertToFullChunk(newHolder);
            // running all pending tasks
            mainThreadExecutor.runTasks(() -> mainThreadExecutor.getTaskCount() == 0);
            //worldgenExecutor.
            // save all pending chunks / potentially only one max - ours
            // hopefully that would flush all chunk writes, including current chink
            while ((runnable = field_19343.poll()) != null)
            {
                runnable.run();
            }
            convertToFullChunk(newHolder);
        }
        mainThreadExecutor.runTasks(() -> mainThreadExecutor.getTaskCount() == 0);
        //worldgenExecutor.
        // save all pending chunks / potentially only one max - ours
        // hopefully that would flush all chunk writes, including current chink
        while ((runnable = field_19343.poll()) != null)
        {
            runnable.run();
        }
    }
}
