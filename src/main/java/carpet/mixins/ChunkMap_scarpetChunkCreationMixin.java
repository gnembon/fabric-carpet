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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkHolder.ChunkLoadingFailure;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ChunkMap.DistanceManager;
import net.minecraft.server.level.ChunkTaskPriorityQueueSorter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.storage.RegionFile;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.datafixers.util.Either;

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
    private LongSet entitiesInLevel;

    @Shadow
    @Final
    private Long2ObjectLinkedOpenHashMap<ChunkHolder> updatingChunkMap;

    @Shadow
    private boolean modified;

    @Shadow
    @Final
    private ThreadedLevelLightEngine lightEngine;

    @Shadow
    @Final
    private ChunkTaskPriorityQueueSorter queueSorter;

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
    protected abstract CompletableFuture<Either<List<ChunkAccess>, ChunkLoadingFailure>> getChunkRangeFuture (final ChunkPos centerChunk, final int margin, final IntFunction<ChunkStatus> distanceToStatus);

    @Shadow
    protected abstract Iterable<ChunkHolder> getChunks();


    ThreadLocal<Boolean> generated = ThreadLocal.withInitial(() -> null);

    // in protoChunkToFullChunk
    // fancier version of the one below, ensuring that the event is triggered when the chunk is actually loaded.
    @Inject(method = "method_20460", at = @At("HEAD"), remap = false)
    private void onChunkGeneratedStart(ChunkHolder chunkHolder, Either<ChunkAccess, ChunkLoadingFailure> chunk, CallbackInfoReturnable<CompletableFuture<Either<ChunkAccess, ChunkLoadingFailure>>> cir)
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

    @Inject(method = "method_20460", at = @At("RETURN"), remap = false)
    private void onChunkGeneratedEnd(ChunkHolder chunkHolder, Either<ChunkAccess, ChunkLoadingFailure> chunk, CallbackInfoReturnable<CompletableFuture<Either<ChunkAccess, ChunkLoadingFailure>>> cir)
    {
        Boolean localGenerated= generated.get();
        if (localGenerated != null)
        {
            MinecraftServer server =  this.level.getServer();
            int ticks = server.getTickCount();
            ChunkPos chpos = chunkHolder.getPos();
            // need to send these because if an app does something with that event, it may lock the thread
            // so better be safe and schedule it for later, aSaP
            if (CHUNK_GENERATED.isNeeded() && localGenerated)
               server.tell(new TickTask(ticks, () -> CHUNK_GENERATED.onChunkEvent(this.level, chpos, true)));
            if (CHUNK_LOADED.isNeeded())
               server.tell(new TickTask(ticks, () -> CHUNK_LOADED.onChunkEvent(this.level, chpos, localGenerated)));
        }
    }

    /* simple but a version that doesn't guarantee that the chunk is actually loaded
    @Inject(method = "convertToFullChunk", at = @At("HEAD"))
    private void onChunkGeneratedEnd(ChunkHolder chunkHolder, CallbackInfoReturnable<CompletableFuture<Either<Chunk, Unloaded>>> cir)
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
    private void addTicket(final ChunkPos pos, final ChunkStatus status)
    {  // UNKNOWN
        this.distanceManager.addTicket(TicketType.UNKNOWN, pos, 33 + ChunkStatus.getDistance(status), pos);
    }

    @Unique
    private void addTicket(final ChunkPos pos)
    {
        this.addTicket(pos, ChunkStatus.EMPTY);
    }

    @Unique
    private void addRelightTicket(final ChunkPos pos)
    {
        this.distanceManager.addRegionTicket(TicketType.LIGHT, pos, 1, pos);
    }

    @Override
    public void releaseRelightTicket(final ChunkPos pos)
    {
        this.mainThreadExecutor.tell(Util.name(
            () -> this.distanceManager.removeRegionTicket(TicketType.LIGHT, pos, 1, pos),
            () -> "release relight ticket " + pos
        ));
    }

    @Unique
    private void tickTicketManager()
    {
        this.distanceManager.runAllUpdates((ChunkMap) (Object) this);
    }

    @Unique
    private Set<ChunkPos> getExistingChunks(final Set<ChunkPos> requestedChunks)
    {
        final Map<String, RegionFile> regionCache = new HashMap<>();
        final Set<ChunkPos> ret = new HashSet<>();

        for (final ChunkPos pos : requestedChunks)
            if (WorldTools.canHasChunk(this.level, pos, regionCache, true))
                ret.add(pos);

        return ret;
    }

    @Unique
    private Set<ChunkPos> loadExistingChunksFromDisk(final Set<ChunkPos> requestedChunks)
    {
        final Set<ChunkPos> existingChunks = this.getExistingChunks(requestedChunks);
        for (final ChunkPos pos : existingChunks)
            this.updatingChunkMap.get(pos.toLong()).getOrScheduleFuture(ChunkStatus.EMPTY, (ChunkMap) (Object) this);

        return existingChunks;
    }

    @Unique
    private Set<ChunkPos> loadExistingChunks(final Set<ChunkPos> requestedChunks, final Object2IntMap<String> report)
    {
        if (report != null)
            report.put("requested_chunks", requestedChunks.size());

        // Load all relevant ChunkHolders into this.currentChunkHolders
        // This will not trigger loading from disk yet

        for (final ChunkPos pos : requestedChunks)
            this.addTicket(pos);

        this.tickTicketManager();

        // Fetch all currently loaded chunks

        final Set<ChunkPos> loadedChunks = requestedChunks.stream().filter(
            pos -> this.updatingChunkMap.get(pos.toLong()).getLastAvailable() != null // all relevant ChunkHolders exist
        ).collect(Collectors.toSet());

        if (report != null)
            report.put("loaded_chunks", loadedChunks.size());

        // Load remaining chunks from disk

        final Set<ChunkPos> unloadedChunks = new HashSet<>(requestedChunks);
        unloadedChunks.removeAll(loadedChunks);

        final Set<ChunkPos> existingChunks = this.loadExistingChunksFromDisk(unloadedChunks);

        existingChunks.addAll(loadedChunks);

        return existingChunks;
    }

    @Unique
    private Set<ChunkPos> loadExistingChunks(final Set<ChunkPos> requestedChunks)
    {
        return this.loadExistingChunks(requestedChunks, null);
    }

    @Unique
    private void waitFor(final Future<?> future)
    {
        this.mainThreadExecutor.managedBlock(future::isDone);
    }

    @Unique
    private void waitFor(final List<? extends CompletableFuture<?>> futures)
    {
        this.waitFor(Util.sequenceFailFast(futures));
    }

    @Unique
    private ChunkAccess getCurrentChunk(final ChunkPos pos)
    {
        final CompletableFuture<ChunkAccess> future = this.updatingChunkMap.get(pos.toLong()).getChunkToSave();
        this.waitFor(future);

        return future.join();
    }

    @Override
    public void relightChunk(ChunkPos pos)
    {
        this.addTicket(pos);
        this.tickTicketManager();
        if (this.updatingChunkMap.get(pos.toLong()).getLastAvailable() == null) // chunk unloaded
            if (WorldTools.canHasChunk(this.level, pos, null, true))
                this.updatingChunkMap.get(pos.toLong()).getOrScheduleFuture(ChunkStatus.EMPTY, (ChunkMap) (Object) this);
        final ChunkAccess chunk = this.getCurrentChunk(pos);
        if (!(chunk.getStatus().isOrAfter(ChunkStatus.LIGHT.getParent()))) return;
        ((ServerLightingProviderInterface) this.lightEngine).removeLightData(chunk);
        this.addRelightTicket(pos);
        final CompletableFuture<?> lightFuture = this.getChunkRangeFuture (pos, 1, (pos_) -> ChunkStatus.LIGHT)
                .thenCompose(
                    either -> either.map(
                            list -> ((ServerLightingProviderInterface) this.lightEngine).relight(chunk),
                            unloaded -> {
                                this.releaseRelightTicket(pos);
                                return CompletableFuture.completedFuture(null);
                            }
                    )
                );
        this.waitFor(lightFuture);
    }

    @Override
    public Map<String, Integer> regenerateChunkRegion(final List<ChunkPos> requestedChunksList)
    {
        final Object2IntMap<String> report = new Object2IntOpenHashMap<>();
        final Set<ChunkPos> requestedChunks = new HashSet<>(requestedChunksList);

        // Load requested chunks

        final Set<ChunkPos> existingChunks = this.loadExistingChunks(requestedChunks, report);

        // Finish pending generation stages
        // This ensures that no generation events will be put back on the main thread after the chunks have been deleted

        final Set<ChunkAccess> affectedChunks = new HashSet<>();

        for (final ChunkPos pos : existingChunks)
            affectedChunks.add(this.getCurrentChunk(pos));

        report.put("affected_chunks", affectedChunks.size());

        // Load neighbors for light removal

        final Set<ChunkPos> neighbors = new HashSet<>();

        for (final ChunkAccess chunk : affectedChunks)
        {
            final ChunkPos pos = chunk.getPos();

            for (int x = -1; x <= 1; ++x)
                for (int z = -1; z <= 1; ++z)
                    if (x != 0 || z != 0)
                    {
                        final ChunkPos nPos = new ChunkPos(pos.x + x, pos.z + z);
                        if (!requestedChunks.contains(nPos))
                            neighbors.add(nPos);
                    }
        }

        this.loadExistingChunks(neighbors);

        // Determine affected neighbors

        final Set<ChunkAccess> affectedNeighbors = new HashSet<>();

        for (final ChunkPos pos : neighbors)
        {
            final ChunkAccess chunk = this.getCurrentChunk(pos);

            if (chunk.getStatus().isOrAfter(ChunkStatus.LIGHT.getParent()))
                affectedNeighbors.add(chunk);
        }

        // Unload affected chunks

        for (final ChunkAccess chunk : affectedChunks)
        {
            final ChunkPos pos = chunk.getPos();

            // remove entities
            long longPos = pos.toLong();
            if (this.entitiesInLevel.contains(longPos) && chunk instanceof LevelChunk)
                ((SimpleEntityLookupInterface<Entity>)((ServerWorldInterface)level).getEntityLookupCMPublic()).getChunkEntities(pos).forEach(entity -> { if (!(entity instanceof Player)) entity.discard();});


            if (chunk instanceof LevelChunk)
                ((LevelChunk) chunk).setLoaded(false);

            if (this.entitiesInLevel.remove(pos.toLong()) && chunk instanceof LevelChunk)
                this.level.unload((LevelChunk) chunk); // block entities only

            ((ServerLightingProviderInterface) this.lightEngine).invokeUpdateChunkStatus(pos);
            ((ServerLightingProviderInterface) this.lightEngine).removeLightData(chunk);

            this.progressListener.onStatusChange(pos, null);
        }

        // Replace ChunkHolders

        for (final ChunkAccess chunk : affectedChunks)
        {
            final ChunkPos cPos = chunk.getPos();
            final long pos = cPos.toLong();

            final ChunkHolder oldHolder = this.updatingChunkMap.remove(pos);
            final ChunkHolder newHolder = new ChunkHolder(cPos, oldHolder.getTicketLevel(), level, this.lightEngine, this.queueSorter, (ChunkHolder.PlayerProvider) this);
            ((ChunkHolderInterface) newHolder).setDefaultProtoChunk(cPos, this.mainThreadExecutor, level); // enable chunk blending?
            this.updatingChunkMap.put(pos, newHolder);

            ((ChunkTicketManagerInterface) this.distanceManager).replaceHolder(oldHolder, newHolder);
        }

        this.modified = true;
        this.promoteChunkMap();

        // Remove light for affected neighbors

        for (final ChunkAccess chunk : affectedNeighbors)
            ((ServerLightingProviderInterface) this.lightEngine).removeLightData(chunk);

        // Schedule relighting of neighbors

        for (final ChunkAccess chunk : affectedNeighbors)
            this.addRelightTicket(chunk.getPos());

        this.tickTicketManager();

        final List<CompletableFuture<?>> lightFutures = new ArrayList<>();

        for (final ChunkAccess chunk : affectedNeighbors)
        {
            final ChunkPos pos = chunk.getPos();

            lightFutures.add(this.getChunkRangeFuture (pos, 1, (pos_) -> ChunkStatus.LIGHT).thenCompose(
                either -> either.map(
                    list -> ((ServerLightingProviderInterface) this.lightEngine).relight(chunk),
                    unloaded -> {
                        this.releaseRelightTicket(pos);
                        return CompletableFuture.completedFuture(null);
                    }
                )
            ));
        }

        // Force generation to previous states
        // This ensures that the world is in a consistent state after this method
        // Also, this is needed to ensure chunks are saved to disk

        final Map<ChunkPos, ChunkStatus> targetGenerationStatus = affectedChunks.stream().collect(
            Collectors.toMap(ChunkAccess::getPos, ChunkAccess::getStatus)
        );

        for (final Entry<ChunkPos, ChunkStatus> entry : targetGenerationStatus.entrySet())
            this.addTicket(entry.getKey(), entry.getValue());

        this.tickTicketManager();

        final List<Pair<ChunkStatus, CompletableFuture<?>>> targetGenerationFutures = new ArrayList<>();

        for (final Entry<ChunkPos, ChunkStatus> entry : targetGenerationStatus.entrySet())
            targetGenerationFutures.add(Pair.of(
                entry.getValue(),
                this.updatingChunkMap.get(entry.getKey().toLong()).getOrScheduleFuture(entry.getValue(), (ChunkMap) (Object) this)
            ));

        final Map<ChunkStatus, List<CompletableFuture<?>>> targetGenerationFuturesGrouped = targetGenerationFutures.stream().collect(
            Collectors.groupingBy(
                Pair::getKey,
                Collectors.mapping(
                    Entry::getValue,
                    Collectors.toList()
                )
            )
        );

        for (final ChunkStatus status : ChunkStatus.getStatusList())
        {
            final List<CompletableFuture<?>> futures = targetGenerationFuturesGrouped.get(status);

            if (futures == null)
                continue;

            report.put("layer_count_" + status.getName(), futures.size());
            final long start = System.currentTimeMillis();

            this.waitFor(futures);

            report.put("layer_time_" + status.getName(), (int) (System.currentTimeMillis() - start));
        }

        report.put("relight_count", lightFutures.size());
        final long relightStart = System.currentTimeMillis();

        this.waitFor(lightFutures);

        report.put("relight_time", (int) (System.currentTimeMillis() - relightStart));

        return report;
    }

    @Override
    public Iterable<ChunkHolder> getChunksCM() {
        return getChunks();
    }
}
