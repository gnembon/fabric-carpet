package carpet.mixins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import carpet.fakes.SimpleEntityLookupInterface;
import carpet.fakes.ServerWorldInterface;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ChunkMap.DistanceManager;
import net.minecraft.server.level.ChunkResult;
//import net.minecraft.server.level.ChunkTaskPriorityQueueSorter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.storage.RegionFile;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import carpet.fakes.ChunkHolderInterface;
import carpet.fakes.ChunkTicketManagerInterface;
import carpet.fakes.ServerLightingProviderInterface;
import carpet.fakes.ThreadedAnvilChunkStorageInterface;
import carpet.script.utils.WorldTools;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import static carpet.script.CarpetEventServer.Event.CHUNK_GENERATED;
import static carpet.script.CarpetEventServer.Event.CHUNK_LOADED;

@Mixin(ChunkMap.class)
public abstract class ChunkMap_scarpetChunkCreationMixin implements ThreadedAnvilChunkStorageInterface
{
    @Shadow
    @Final
    private ServerLevel level;

    @Shadow
    @Final
    private Long2ObjectLinkedOpenHashMap<ChunkHolder> updatingChunkMap;

    @Shadow
    private boolean modified;

    @Shadow
    @Final
    private ThreadedLevelLightEngine lightEngine;

    //@Shadow
    //@Final
    //private ChunkTaskPriorityQueueSorter queueSorter;

    @Shadow
    @Final
    private BlockableEventLoop<Runnable> mainThreadExecutor;

    @Shadow
    @Final
    private ChunkProgressListener progressListener;

    @Shadow
    @Final
    private DistanceManager distanceManager;

    @Shadow
    protected abstract boolean promoteChunkMap();

    @Shadow
    protected abstract Iterable<ChunkHolder> getChunks();


    @Shadow
    protected abstract CompletableFuture<ChunkResult<List<ChunkAccess>>> getChunkRangeFuture(ChunkHolder chunkHolder, int i, IntFunction<ChunkStatus> intFunction);

    //@Shadow protected abstract void postLoadProtoChunk(final ServerLevel serverLevel, final List<CompoundTag> list);

    ThreadLocal<Boolean> generated = ThreadLocal.withInitial(() -> null);

    // in protoChunkToFullChunk
    // fancier version of the one below, ensuring that the event is triggered when the chunk is actually loaded.

    /*


    @Inject(method = "method_17227", at = @At("HEAD"), remap = false)
    private void onChunkGeneratedStart(ChunkHolder chunkHolder, ChunkAccess chunkAccess, CallbackInfoReturnable<ChunkAccess> cir)
    {
        if (CHUNK_GENERATED.isNeeded() || CHUNK_LOADED.isNeeded())
        {
            generated.set(chunkHolder.getLastAvailable().getStatus() != ChunkStatus.FULL);
        }
        else
        {
            generated.set(null);
        }
    }

    @Inject(method = "method_17227", at = @At("RETURN"), remap = false)
    private void onChunkGeneratedEnd(ChunkHolder chunkHolder, ChunkAccess chunk, CallbackInfoReturnable<CompletableFuture<ChunkResult<ChunkAccess>>> cir)
    {
        Boolean localGenerated = generated.get();
        if (localGenerated != null)
        {
            MinecraftServer server = this.level.getServer();
            int ticks = server.getTickCount();
            ChunkPos chpos = chunkHolder.getPos();
            // need to send these because if an app does something with that event, it may lock the thread
            // so better be safe and schedule it for later, aSaP
            if (CHUNK_GENERATED.isNeeded() && localGenerated)
            {
                server.tell(new TickTask(ticks, () -> CHUNK_GENERATED.onChunkEvent(this.level, chpos, true)));
            }
            if (CHUNK_LOADED.isNeeded())
            {
                server.tell(new TickTask(ticks, () -> CHUNK_LOADED.onChunkEvent(this.level, chpos, localGenerated)));
            }
        }
    }

     */

    /* simple but a version that doesn't guarantee that the chunk is actually loaded
    @Inject(method = "convertToFullChunk", at = @At("HEAD"))
    private void onChunkGeneratedEnd(ChunkHolder chunkHolder, CallbackInfoReturnable<CompletableFuture<ChunkResult<ChunkAccess>>> cir)
    {
        if (CHUNK_GENERATED.isNeeded() && chunkHolder.getCurrentChunk().getStatus() != ChunkStatus.FULL)
        {
            ChunkPos chpos = chunkHolder.getPos();
            this.world.getServer().execute(() -> CHUNK_GENERATED.onChunkEvent(this.world, chpos, true));
        }
        if (CHUNK_LOADED.isNeeded())
        {
            boolean generated = chunkHolder.getCurrentChunk().getStatus() != ChunkStatus.FULL;
            ChunkPos chpos = chunkHolder.getPos();
            this.world.getServer().execute(() -> CHUNK_LOADED.onChunkEvent(this.world, chpos, generated));
        }
    }
     */

    @Unique
    private void addTicket(ChunkPos pos, ChunkStatus status)
    {  // UNKNOWN
        this.distanceManager.addTicket(TicketType.UNKNOWN, pos, 33 + ChunkLevel.byStatus(status), pos);
    }

    @Unique
    private void addTicket(ChunkPos pos)
    {
        this.addTicket(pos, ChunkStatus.EMPTY);
    }


    /*
    @Unique
    private void addRelightTicket(ChunkPos pos)
    {
        this.distanceManager.addRegionTicket(TicketType.LIGHT, pos, 1, pos);
    }

    @Override
    public void releaseRelightTicket(ChunkPos pos)
    {
        this.mainThreadExecutor.tell(Util.name(
                () -> this.distanceManager.removeRegionTicket(TicketType.LIGHT, pos, 1, pos),
                () -> "release relight ticket " + pos
        ));
    }
     */
    @Unique
    private void tickTicketManager()
    {
        this.distanceManager.runAllUpdates((ChunkMap) (Object) this);
    }

    @Unique
    private Set<ChunkPos> getExistingChunks(Set<ChunkPos> requestedChunks)
    {
        Map<String, RegionFile> regionCache = new HashMap<>();
        Set<ChunkPos> ret = new HashSet<>();

        for (ChunkPos pos : requestedChunks)
        {
            if (WorldTools.canHasChunk(this.level, pos, regionCache, true))
            {
                ret.add(pos);
            }
        }

        return ret;
    }


    /*
    @Unique
    private Set<ChunkPos> loadExistingChunksFromDisk(Set<ChunkPos> requestedChunks)
    {
        Set<ChunkPos> existingChunks = this.getExistingChunks(requestedChunks);
        for (ChunkPos pos : existingChunks)
        {
            this.updatingChunkMap.get(pos.toLong()).getOrScheduleFuture(ChunkStatus.EMPTY, (ChunkMap) (Object) this);
        }

        return existingChunks;
    }

    @Unique
    private Set<ChunkPos> loadExistingChunks(Set<ChunkPos> requestedChunks, Object2IntMap<String> report)
    {
        if (report != null)
        {
            report.put("requested_chunks", requestedChunks.size());
        }

        // Load all relevant ChunkHolders into this.currentChunkHolders
        // This will not trigger loading from disk yet

        for (ChunkPos pos : requestedChunks)
        {
            this.addTicket(pos);
        }

        this.tickTicketManager();

        // Fetch all currently loaded chunks

        Set<ChunkPos> loadedChunks = requestedChunks.stream().filter(
                pos -> this.updatingChunkMap.get(pos.toLong()).getLastAvailable() != null // all relevant ChunkHolders exist
        ).collect(Collectors.toSet());

        if (report != null)
        {
            report.put("loaded_chunks", loadedChunks.size());
        }

        // Load remaining chunks from disk

        Set<ChunkPos> unloadedChunks = new HashSet<>(requestedChunks);
        unloadedChunks.removeAll(loadedChunks);

        Set<ChunkPos> existingChunks = this.loadExistingChunksFromDisk(unloadedChunks);

        existingChunks.addAll(loadedChunks);

        return existingChunks;
    }

    @Unique
    private Set<ChunkPos> loadExistingChunks(Set<ChunkPos> requestedChunks)
    {
        return this.loadExistingChunks(requestedChunks, null);
    }

    @Unique
    private void waitFor(Future<?> future)
    {
        this.mainThreadExecutor.managedBlock(future::isDone);
    }

    @Unique
    private void waitFor(List<? extends CompletableFuture<?>> futures)
    {
        this.waitFor(Util.sequenceFailFast(futures));
    }

    @Unique
    private ChunkAccess getCurrentChunk(ChunkPos pos)
    {
        CompletableFuture<ChunkAccess> future = this.updatingChunkMap.get(pos.toLong()).getChunkToSave();
        this.waitFor(future);

        return future.join();
    }

    @Override
    public void relightChunk(ChunkPos pos)
    {
        this.addTicket(pos);
        this.tickTicketManager();
        if (this.updatingChunkMap.get(pos.toLong()).getLastAvailable() == null) // chunk unloaded
        {
            if (WorldTools.canHasChunk(this.level, pos, null, true))
            {
                this.updatingChunkMap.get(pos.toLong()).getOrScheduleFuture(ChunkStatus.EMPTY, (ChunkMap) (Object) this);
            }
        }
        ChunkAccess chunk = this.getCurrentChunk(pos);
        if (!(chunk.getStatus().isOrAfter(ChunkStatus.LIGHT.getParent())))
        {
            return;
        }
        ((ServerLightingProviderInterface) this.lightEngine).removeLightData(chunk);
        this.addRelightTicket(pos);
        ChunkHolder chunkHolder = this.updatingChunkMap.get(pos.toLong());
        CompletableFuture<?> lightFuture = this.getChunkRangeFuture(chunkHolder, 1, (pos_) -> ChunkStatus.LIGHT)
                .thenCompose(results -> {
                            List<ChunkAccess> depList = results.orElse(null);
                            if (depList == null)
                            {
                                this.releaseRelightTicket(pos);
                                return CompletableFuture.completedFuture(ChunkResult.error(results::getError));
                            }
                            ((ServerLightingProviderInterface) this.lightEngine).relight(chunk);
                            return CompletableFuture.completedFuture(ChunkResult.of(depList));
                        }
                );
        this.waitFor(lightFuture);
    }

    /*

    @Override
    public Map<String, Integer> regenerateChunkRegion(List<ChunkPos> requestedChunksList)
    {
        Object2IntMap<String> report = new Object2IntOpenHashMap<>();
        Set<ChunkPos> requestedChunks = new HashSet<>(requestedChunksList);

        // Load requested chunks

        Set<ChunkPos> existingChunks = this.loadExistingChunks(requestedChunks, report);

        // Finish pending generation stages
        // This ensures that no generation events will be put back on the main thread after the chunks have been deleted

        Set<ChunkAccess> affectedChunks = new HashSet<>();

        for (ChunkPos pos : existingChunks)
        {
            affectedChunks.add(this.getCurrentChunk(pos));
        }

        report.put("affected_chunks", affectedChunks.size());

        // Load neighbors for light removal

        Set<ChunkPos> neighbors = new HashSet<>();

        for (ChunkAccess chunk : affectedChunks)
        {
            ChunkPos pos = chunk.getPos();

            for (int x = -1; x <= 1; ++x)
            {
                for (int z = -1; z <= 1; ++z)
                {
                    if (x != 0 || z != 0)
                    {
                        ChunkPos nPos = new ChunkPos(pos.x + x, pos.z + z);
                        if (!requestedChunks.contains(nPos))
                        {
                            neighbors.add(nPos);
                        }
                    }
                }
            }
        }

        this.loadExistingChunks(neighbors);

        // Determine affected neighbors

        Set<ChunkAccess> affectedNeighbors = new HashSet<>();

        for (ChunkPos pos : neighbors)
        {
            ChunkAccess chunk = this.getCurrentChunk(pos);

            if (chunk.getPersistedStatus().isOrAfter(ChunkStatus.LIGHT.getParent()))
            {
                affectedNeighbors.add(chunk);
            }
        }

        // Unload affected chunks

        for (ChunkAccess chunk : affectedChunks)
        {
            ChunkPos pos = chunk.getPos();

            // remove entities
            long longPos = pos.toLong();
            if (chunk instanceof LevelChunk)
            {
                ((SimpleEntityLookupInterface<Entity>) ((ServerWorldInterface) level).getEntityLookupCMPublic()).getChunkEntities(pos).forEach(entity -> {
                    if (!(entity instanceof Player))
                    {
                        entity.discard();
                    }
                });
            }


            if (chunk instanceof LevelChunk)
            {
                ((LevelChunk) chunk).setLoaded(false);
            }

            if (chunk instanceof LevelChunk)
            {
                this.level.unload((LevelChunk) chunk); // block entities only
            }

            ((ServerLightingProviderInterface) this.lightEngine).invokeUpdateChunkStatus(pos);
            ((ServerLightingProviderInterface) this.lightEngine).removeLightData(chunk);

            this.progressListener.onStatusChange(pos, null);
        }

        // Replace ChunkHolders

        for (ChunkAccess chunk : affectedChunks)
        {
            ChunkPos cPos = chunk.getPos();
            long pos = cPos.toLong();

            ChunkHolder oldHolder = this.updatingChunkMap.remove(pos);
            ChunkHolder newHolder = new ChunkHolder(cPos, oldHolder.getTicketLevel(), level, this.lightEngine, this.queueSorter, (ChunkHolder.PlayerProvider) this);
            ((ChunkHolderInterface) newHolder).setDefaultProtoChunk(cPos, this.mainThreadExecutor, level); // enable chunk blending?
            this.updatingChunkMap.put(pos, newHolder);

            ((ChunkTicketManagerInterface) this.distanceManager).replaceHolder(oldHolder, newHolder);
        }

        this.modified = true;
        this.promoteChunkMap();


        // Force generation to previous states
        // This ensures that the world is in a consistent state after this method
        // Also, this is needed to ensure chunks are saved to disk

        Map<ChunkPos, ChunkStatus> targetGenerationStatus = affectedChunks.stream().collect(
                Collectors.toMap(ChunkAccess::getPos, ChunkAccess::getPersistedStatus)
        );

        for (Entry<ChunkPos, ChunkStatus> entry : targetGenerationStatus.entrySet())
        {
            this.addTicket(entry.getKey(), entry.getValue());
        }

        this.tickTicketManager();

        List<Pair<ChunkStatus, CompletableFuture<?>>> targetGenerationFutures = new ArrayList<>();

        for (Entry<ChunkPos, ChunkStatus> entry : targetGenerationStatus.entrySet())
        {
            targetGenerationFutures.add(Pair.of(
                    entry.getValue(),
                    this.updatingChunkMap.get(entry.getKey().toLong()).getOrScheduleFuture(entry.getValue(), (ChunkMap) (Object) this)
            ));
        }

        Map<ChunkStatus, List<CompletableFuture<?>>> targetGenerationFuturesGrouped = targetGenerationFutures.stream().collect(
                Collectors.groupingBy(
                        Pair::getKey,
                        Collectors.mapping(
                                Entry::getValue,
                                Collectors.toList()
                        )
                )
        );

        for (ChunkStatus status : ChunkStatus.getStatusList())
        {
            List<CompletableFuture<?>> futures = targetGenerationFuturesGrouped.get(status);

            if (futures == null)
            {
                continue;
            }

            String statusName = BuiltInRegistries.CHUNK_STATUS.getKey(status).getPath();

            report.put("layer_count_" + statusName, futures.size());
            long start = System.currentTimeMillis();

            this.waitFor(futures);

            report.put("layer_time_" + statusName, (int) (System.currentTimeMillis() - start));
        }


        report.put("relight_count", affectedNeighbors.size());

        // Remove light for affected neighbors

        for (ChunkAccess chunk : affectedNeighbors)
        {
            ((ServerLightingProviderInterface) this.lightEngine).removeLightData(chunk);
        }

        // Schedule relighting of neighbors

        for (ChunkAccess chunk : affectedNeighbors)
        {
            this.addRelightTicket(chunk.getPos());
        }

        this.tickTicketManager();

        List<CompletableFuture<?>> lightFutures = new ArrayList<>();

        for (ChunkAccess chunk : affectedNeighbors)
        {
            ChunkPos pos = chunk.getPos();

            lightFutures.add(this.getChunkRangeFuture(this.updatingChunkMap.get(pos.toLong()), 1, (pos_) -> ChunkStatus.LIGHT).thenCompose(
                    results -> {
                        List<ChunkAccess> depList = results.orElse(null);
                        if (depList == null)
                        {
                            this.releaseRelightTicket(pos);
                            return CompletableFuture.completedFuture(ChunkResult.error(results::getError));
                        }
                        ((ServerLightingProviderInterface) this.lightEngine).relight(chunk);
                        return CompletableFuture.completedFuture(ChunkResult.of(depList));
                    }
            ));
        }


        long relightStart = System.currentTimeMillis();

        this.waitFor(lightFutures);

        report.put("relight_time", (int) (System.currentTimeMillis() - relightStart));

        return report;
    }



    @Override
    public Iterable<ChunkHolder> getChunksCM()
    {
        return getChunks();
    }

     */
}
