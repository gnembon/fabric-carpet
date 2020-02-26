package carpet.fakes;

import com.mojang.datafixers.util.Either;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.thread.ThreadExecutor;
import net.minecraft.world.chunk.Chunk;

import java.util.concurrent.CompletableFuture;

public interface ChunkHolderInterface
{
    CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> setDefaultProtoChunk(ChunkPos chpos, ThreadExecutor<Runnable> executor);
}
