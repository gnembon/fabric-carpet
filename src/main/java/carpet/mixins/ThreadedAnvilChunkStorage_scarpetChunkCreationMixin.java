package carpet.mixins;

import carpet.fakes.ThreadedAnvilChunkStorageInterface;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ChunkTaskPrioritySystem;
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;

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
    public void regenerateChunk(ChunkPos chpos)
    {
        Long id = chpos.toLong();
        if (loadedChunks.contains(id))
        {
            //unload chunk first
            unloadedChunks.remove(id);
            ChunkHolder chunkHolder = (ChunkHolder)currentChunkHolders.remove(id);
            if (chunkHolder != null) {
                field_18807.put(id, chunkHolder);
                chunkHolderListDirty = true;
                method_20458(id, chunkHolder);
            }
            Runnable runnable;
            while((runnable = (Runnable) field_19343.poll()) != null) {
                runnable.run();
            }
        }
        ChunkHolder newHolder = new ChunkHolder(chpos, 0,serverLightingProvider, chunkTaskPrioritySystem, (ChunkHolder.PlayersWatchingChunkProvider) this);
        this.currentChunkHolders.put(id, newHolder);
        this.chunkHolderListDirty = true;


    }
}
