package carpet.mixins;

import carpet.fakes.ChunkHolderInterface;
import com.mojang.datafixers.util.Either;
import net.minecraft.core.registries.Registries;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReferenceArray;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;

@Mixin(ChunkHolder.class)
public abstract class ChunkHolder_scarpetChunkCreationMixin implements ChunkHolderInterface
{
    @Shadow protected abstract void updateChunkToSave(CompletableFuture<? extends Either<? extends ChunkAccess, ChunkHolder.ChunkLoadingFailure>> newChunkFuture, String type);

    @Shadow @Final private AtomicReferenceArray<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> futures;

    @Override
    public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> setDefaultProtoChunk(ChunkPos chpos, BlockableEventLoop<Runnable> executor, ServerLevel world)
    {
        int i = ChunkStatus.EMPTY.getIndex();
        CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completableFuture2 = CompletableFuture.supplyAsync(
                () -> Either.left(new ProtoChunk(chpos, UpgradeData.EMPTY, world,  world.registryAccess().registryOrThrow(Registries.BIOME), null)), // todo figure out what that does - maybe add an option to reset with blending enabled..?
                executor
        );
        updateChunkToSave(completableFuture2, "unfull"); // possible debug data
        futures.set(i, completableFuture2);
        return completableFuture2;
    }
}
