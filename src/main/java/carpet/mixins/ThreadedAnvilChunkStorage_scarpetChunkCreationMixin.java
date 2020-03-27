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
import net.minecraft.world.storage.RegionFile;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
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

    //in method_20617
    //method_19534(Lnet/minecraft/server/world/ChunkHolder;Lnet/minecraft/world/chunk/Chunk;)Ljava/util/concurrent/CompletableFuture;
    // incmopatibility with optifine makes this mixin fail.
    @Inject(method = "method_19534", require = 0, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ThreadedAnvilChunkStorage;convertToFullChunk(Lnet/minecraft/server/world/ChunkHolder;)Ljava/util/concurrent/CompletableFuture;",
            shift = At.Shift.AFTER
    ))
    private void onChunkGenerated(ChunkHolder chunkHolder, Chunk chunk, CallbackInfoReturnable<CompletableFuture> cir)
    {
        if (CHUNK_GENERATED.isNeeded())
            world.getServer().execute( () -> CHUNK_GENERATED.onChunkGenerated(world, chunk) );
    }

    private boolean chunkExists(ChunkPos chpos, Map<String, RegionFile> regionCache)
    {
        String currentRegionName = "r." + chpos.getRegionX() + "." + chpos.getRegionZ() + ".mca";
        if (regionCache != null && regionCache.containsKey(currentRegionName))
        {
            RegionFile region = regionCache.get(currentRegionName);
            if (region == null) return false;
            return region.hasChunk(chpos);
        }
        Path regionPath = world.getDimension().getType().getSaveDirectory(
                world.getSaveHandler().getWorldDir()
        ).toPath().resolve("region");
        Path regionFilePath = regionPath.resolve(currentRegionName);
        File regionFile = regionFilePath.toFile();
        if (!regionFile.exists())
        {
            if (regionCache != null) regionCache.put(currentRegionName, null);
            return false;
        }
        try
        {
            RegionFile region = new RegionFile(regionFile, regionPath.toFile());
            if (regionCache != null) regionCache.put(currentRegionName, region);
            return region.hasChunk(chpos);
        }
        catch (IOException ignored) { }
        return true;
    }

    @Override
    public Map<String, Integer> regenerateChunkRegion(List<ChunkPos> requestedChunks)
    {
        Map<String, Integer> report = new HashMap<>();
        report.put("requested_chunks",requestedChunks.size());
        //flushning all tasks on the thread executor so all futures are ready - we don't want any deadlocks
        mainThreadExecutor.runTasks(() -> mainThreadExecutor.getTaskCount() == 0);
        // save all pending chunks
        // hopefully that would flush all chunk writes, including current chunk, if exists
        Runnable runnable;
        while((runnable = field_19343.poll()) != null) {
            runnable.run();
        }

        Set<ChunkPos> loadedChunks = new HashSet<>();
        for (ChunkPos pos: requestedChunks)
        {
            Chunk chunk = world.getChunk(pos.x, pos.z, ChunkStatus.FULL, false);
            if (chunk != null && chunk.getStatus() != ChunkStatus.EMPTY)
                loadedChunks.add(pos);
        }
        report.put("loaded_chunks", loadedChunks.size());

        Map<ChunkPos,ChunkStatus> targetStatus = new HashMap<>();
        Map<String, RegionFile> regionCache = new HashMap<>();
        for (ChunkPos pos: requestedChunks)
        {
            if (!chunkExists(pos, regionCache)) continue;
            Chunk chunk = world.getChunk(pos.x, pos.z, ChunkStatus.EMPTY, true);
            if (chunk == null || chunk.getStatus() == ChunkStatus.EMPTY)
                continue;
            //unloaded areas can have unknown skirt chunk area
            targetStatus.put(pos, loadedChunks.contains(pos)?chunk.getStatus():ChunkStatus.STRUCTURE_STARTS);
        }
        regionCache.clear();
        report.put("affected_chunks", targetStatus.size());


        for (ChunkPos chpos : targetStatus.keySet())
        {
            Long id = chpos.toLong();
            //chunk is currently in memory in some shape or form / loaded / unloaded / queued / cached
            ChunkHolder chunkHolder = currentChunkHolders.remove(id);
            int currentLevel = ThreadedAnvilChunkStorage.MAX_LEVEL;
            if (chunkHolder != null)
            {
                currentLevel = chunkHolder.getLevel()-1;
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
            ChunkHolder newHolder = new ChunkHolder(chpos, currentLevel,serverLightingProvider, chunkTaskPrioritySystem, (ChunkHolder.PlayersWatchingChunkProvider) this);
            ((ChunkHolderInterface)newHolder).setDefaultProtoChunk(chpos, mainThreadExecutor);
            this.currentChunkHolders.put(id, newHolder);
            //removed holders
            field_18807.put(id, newHolder);
            //loadedChunks.add(id);

        }
        //getting rid of all signs
        this.chunkHolderListDirty = true;
        // not needed right now
        mainThreadExecutor.runTasks(() -> mainThreadExecutor.getTaskCount() == 0);
        //worldgenExecutor.
        // save all pending chunks / potentially only one max - ours
        // hopefully that would flush all chunk writes, including current chink
        while ((runnable = field_19343.poll()) != null)
        {
            runnable.run();
        }

        for (ChunkStatus status : ChunkStatus.createOrderedList())
        {
            report.put("layer_count_"+status.getId(),targetStatus.size());
            long start = System.currentTimeMillis();
            if (status == ChunkStatus.LIGHT)
            {
                serverLightingProvider.setTaskBatchSize(targetStatus.size() + 20);
            }
            for (ChunkPos chpos : new ArrayList<>(targetStatus.keySet()))
            {
                if (targetStatus.get(chpos) == status)
                    targetStatus.remove(chpos);
                world.getChunk(chpos.x, chpos.z, status);
                if (status == ChunkStatus.FULL)
                    convertToFullChunk(currentChunkHolders.get(chpos.toLong()));
            }
            if (status == ChunkStatus.LIGHT)
            { // default task batch size after initial generation is done
                serverLightingProvider.setTaskBatchSize(5);
            }
            report.put("layer_time_"+status.getId(), (int) (System.currentTimeMillis()-start));
        }

        mainThreadExecutor.runTasks(() -> mainThreadExecutor.getTaskCount() == 0);
        //worldgenExecutor.
        // save all pending chunks / potentially only one max - ours
        // hopefully that would flush all chunk writes, including current chink
        while ((runnable = field_19343.poll()) != null)
        {
            runnable.run();
        }
        return report;
    }
}
