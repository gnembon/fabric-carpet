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
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTask;
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
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ChunkHolder.Unloaded;
import net.minecraft.server.world.ChunkTaskPrioritySystem;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.server.world.ThreadedAnvilChunkStorage.TicketManager;
import net.minecraft.util.Util;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.thread.ThreadExecutor;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.storage.RegionFile;

import static carpet.script.CarpetEventServer.Event.CHUNK_GENERATED;
import static carpet.script.CarpetEventServer.Event.CHUNK_LOADED;

@Mixin(ThreadedAnvilChunkStorage.class)
public abstract class ThreadedAnvilChunkStorage_scarpetChunkCreationMixin implements ThreadedAnvilChunkStorageInterface
{
    @Shadow
    @Final
    private ServerWorld world;

    @Shadow
    @Final
    private LongSet loadedChunks;

    @Shadow
    @Final
    private Long2ObjectLinkedOpenHashMap<ChunkHolder> currentChunkHolders;

    @Shadow
    private boolean chunkHolderListDirty;

    @Shadow
    @Final
    private ServerLightingProvider lightingProvider;

    @Shadow
    @Final
    private ChunkTaskPrioritySystem chunkTaskPrioritySystem;

    @Shadow
    @Final
    private ThreadExecutor<Runnable> mainThreadExecutor;

    @Shadow
    @Final
    private WorldGenerationProgressListener worldGenerationProgressListener;

    @Shadow
    @Final
    private TicketManager ticketManager;

    @Shadow
    protected abstract boolean updateHolderMap();

    @Shadow
    protected abstract CompletableFuture<Either<List<Chunk>, Unloaded>> getRegion (final ChunkPos centerChunk, final int margin, final IntFunction<ChunkStatus> distanceToStatus);

    @Shadow
    protected abstract Iterable<ChunkHolder> entryIterator();


    ThreadLocal<Boolean> generated = ThreadLocal.withInitial(() -> null);

    // in convertToFullChunk
    // fancier version of the one below, ensuring that the event is triggered when the chunk is actually loaded.
    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(method = "method_20460", at = @At("HEAD"))
    private void onChunkGeneratedStart(ChunkHolder chunkHolder, Either<Chunk, Unloaded> chunk, CallbackInfoReturnable<CompletableFuture<Either<Chunk, Unloaded>>> cir)
    {
        if (CHUNK_GENERATED.isNeeded() || CHUNK_LOADED.isNeeded())
        {
            generated.set(chunkHolder.getCurrentChunk().getStatus() != ChunkStatus.FULL);
        }
        else
        {
            generated.set(null);
        }
    }

    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(method = "method_20460", at = @At("RETURN"))
    private void onChunkGeneratedEnd(ChunkHolder chunkHolder, Either<Chunk, Unloaded> chunk, CallbackInfoReturnable<CompletableFuture<Either<Chunk, Unloaded>>> cir)
    {
        Boolean localGenerated= generated.get();
        if (localGenerated != null)
        {
            MinecraftServer server =  this.world.getServer();
            int ticks = server.getTicks();
            ChunkPos chpos = chunkHolder.getPos();
            // need to send these because if an app does something with that event, it may lock the thread
            // so better be safe and schedule it for later, aSaP
            if (CHUNK_GENERATED.isNeeded() && localGenerated)
               server.send(new ServerTask(ticks, () -> CHUNK_GENERATED.onChunkEvent(this.world, chpos, true)));
            if (CHUNK_LOADED.isNeeded())
               server.send(new ServerTask(ticks, () -> CHUNK_LOADED.onChunkEvent(this.world, chpos, localGenerated)));
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
        this.ticketManager.addTicketWithLevel(ChunkTicketType.UNKNOWN, pos, 33 + ChunkStatus.getDistanceFromFull(status), pos);
    }

    @Unique
    private void addTicket(final ChunkPos pos)
    {
        this.addTicket(pos, ChunkStatus.EMPTY);
    }

    @Unique
    private void addRelightTicket(final ChunkPos pos)
    {
        this.ticketManager.addTicket(ChunkTicketType.LIGHT, pos, 1, pos);
    }

    @Override
    public void releaseRelightTicket(final ChunkPos pos)
    {
        this.mainThreadExecutor.send(Util.debugRunnable(
            () -> this.ticketManager.removeTicket(ChunkTicketType.LIGHT, pos, 1, pos),
            () -> "release relight ticket " + pos
        ));
    }

    @Unique
    private void tickTicketManager()
    {
        this.ticketManager.tick((ThreadedAnvilChunkStorage) (Object) this);
    }

    @Unique
    private Set<ChunkPos> getExistingChunks(final Set<ChunkPos> requestedChunks)
    {
        final Map<String, RegionFile> regionCache = new HashMap<>();
        final Set<ChunkPos> ret = new HashSet<>();

        for (final ChunkPos pos : requestedChunks)
            if (WorldTools.canHasChunk(this.world, pos, regionCache, true))
                ret.add(pos);

        return ret;
    }

    @Unique
    private Set<ChunkPos> loadExistingChunksFromDisk(final Set<ChunkPos> requestedChunks)
    {
        final Set<ChunkPos> existingChunks = this.getExistingChunks(requestedChunks);
        for (final ChunkPos pos : existingChunks)
            this.currentChunkHolders.get(pos.toLong()).getChunkAt(ChunkStatus.EMPTY, (ThreadedAnvilChunkStorage) (Object) this);

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
            pos -> this.currentChunkHolders.get(pos.toLong()).getCurrentChunk() != null // all relevant ChunkHolders exist
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
        this.mainThreadExecutor.runTasks(future::isDone);
    }

    @Unique
    private void waitFor(final List<? extends CompletableFuture<?>> futures)
    {
        this.waitFor(Util.combine(futures));
    }

    @Unique
    private Chunk getCurrentChunk(final ChunkPos pos)
    {
        final CompletableFuture<Chunk> future = this.currentChunkHolders.get(pos.toLong()).getSavingFuture();
        this.waitFor(future);

        return future.join();
    }

    @Override
    public void relightChunk(ChunkPos pos)
    {
        this.addTicket(pos);
        this.tickTicketManager();
        if (this.currentChunkHolders.get(pos.toLong()).getCurrentChunk() == null) // chunk unloaded
            if (WorldTools.canHasChunk(this.world, pos, null, true))
                this.currentChunkHolders.get(pos.toLong()).getChunkAt(ChunkStatus.EMPTY, (ThreadedAnvilChunkStorage) (Object) this);
        final Chunk chunk = this.getCurrentChunk(pos);
        if (!(chunk.getStatus().isAtLeast(ChunkStatus.LIGHT.getPrevious()))) return;
        ((ServerLightingProviderInterface) this.lightingProvider).removeLightData(chunk);
        this.addRelightTicket(pos);
        final CompletableFuture<?> lightFuture = this.getRegion (pos, 1, (pos_) -> ChunkStatus.LIGHT)
                .thenCompose(
                    either -> either.map(
                            list -> ((ServerLightingProviderInterface) this.lightingProvider).relight(chunk),
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

        final Set<Chunk> affectedChunks = new HashSet<>();

        for (final ChunkPos pos : existingChunks)
            affectedChunks.add(this.getCurrentChunk(pos));

        report.put("affected_chunks", affectedChunks.size());

        // Load neighbors for light removal

        final Set<ChunkPos> neighbors = new HashSet<>();

        for (final Chunk chunk : affectedChunks)
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

        final Set<Chunk> affectedNeighbors = new HashSet<>();

        for (final ChunkPos pos : neighbors)
        {
            final Chunk chunk = this.getCurrentChunk(pos);

            if (chunk.getStatus().isAtLeast(ChunkStatus.LIGHT.getPrevious()))
                affectedNeighbors.add(chunk);
        }

        // Unload affected chunks

        for (final Chunk chunk : affectedChunks)
        {
            final ChunkPos pos = chunk.getPos();

            // remove entities
            long longPos = pos.toLong();
            if (this.loadedChunks.contains(longPos) && chunk instanceof WorldChunk)
                ((SimpleEntityLookupInterface<Entity>)((ServerWorldInterface)world).getEntityLookupCMPublic()).getChunkEntities(pos).forEach(entity -> { if (!(entity instanceof PlayerEntity)) entity.discard();});


            if (chunk instanceof WorldChunk)
                ((WorldChunk) chunk).setLoadedToWorld(false);

            if (this.loadedChunks.remove(pos.toLong()) && chunk instanceof WorldChunk)
                this.world.unloadEntities((WorldChunk) chunk); // block entities only

            ((ServerLightingProviderInterface) this.lightingProvider).invokeUpdateChunkStatus(pos);
            ((ServerLightingProviderInterface) this.lightingProvider).removeLightData(chunk);

            this.worldGenerationProgressListener.setChunkStatus(pos, null);
        }

        // Replace ChunkHolders

        for (final Chunk chunk : affectedChunks)
        {
            final ChunkPos cPos = chunk.getPos();
            final long pos = cPos.toLong();

            final ChunkHolder oldHolder = this.currentChunkHolders.remove(pos);
            final ChunkHolder newHolder = new ChunkHolder(cPos, oldHolder.getLevel(), world, this.lightingProvider, this.chunkTaskPrioritySystem, (ChunkHolder.PlayersWatchingChunkProvider) this);
            ((ChunkHolderInterface) newHolder).setDefaultProtoChunk(cPos, this.mainThreadExecutor, world);
            this.currentChunkHolders.put(pos, newHolder);

            ((ChunkTicketManagerInterface) this.ticketManager).replaceHolder(oldHolder, newHolder);
        }

        this.chunkHolderListDirty = true;
        this.updateHolderMap();

        // Remove light for affected neighbors

        for (final Chunk chunk : affectedNeighbors)
            ((ServerLightingProviderInterface) this.lightingProvider).removeLightData(chunk);

        // Schedule relighting of neighbors

        for (final Chunk chunk : affectedNeighbors)
            this.addRelightTicket(chunk.getPos());

        this.tickTicketManager();

        final List<CompletableFuture<?>> lightFutures = new ArrayList<>();

        for (final Chunk chunk : affectedNeighbors)
        {
            final ChunkPos pos = chunk.getPos();

            lightFutures.add(this.getRegion (pos, 1, (pos_) -> ChunkStatus.LIGHT).thenCompose(
                either -> either.map(
                    list -> ((ServerLightingProviderInterface) this.lightingProvider).relight(chunk),
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
            Collectors.toMap(Chunk::getPos, Chunk::getStatus)
        );

        for (final Entry<ChunkPos, ChunkStatus> entry : targetGenerationStatus.entrySet())
            this.addTicket(entry.getKey(), entry.getValue());

        this.tickTicketManager();

        final List<Pair<ChunkStatus, CompletableFuture<?>>> targetGenerationFutures = new ArrayList<>();

        for (final Entry<ChunkPos, ChunkStatus> entry : targetGenerationStatus.entrySet())
            targetGenerationFutures.add(Pair.of(
                entry.getValue(),
                this.currentChunkHolders.get(entry.getKey().toLong()).getChunkAt(entry.getValue(), (ThreadedAnvilChunkStorage) (Object) this)
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

        for (final ChunkStatus status : ChunkStatus.createOrderedList())
        {
            final List<CompletableFuture<?>> futures = targetGenerationFuturesGrouped.get(status);

            if (futures == null)
                continue;

            report.put("layer_count_" + status.getId(), futures.size());
            final long start = System.currentTimeMillis();

            this.waitFor(futures);

            report.put("layer_time_" + status.getId(), (int) (System.currentTimeMillis() - start));
        }

        report.put("relight_count", lightFutures.size());
        final long relightStart = System.currentTimeMillis();

        this.waitFor(lightFutures);

        report.put("relight_time", (int) (System.currentTimeMillis() - relightStart));

        return report;
    }

    @Override
    public Iterable<ChunkHolder> getChunks() {
        return entryIterator();
    }
}
