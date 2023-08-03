package carpet.fakes;

import com.mojang.datafixers.util.Either;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;

public interface ChunkHolderInterface
{
    default CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> carpet$setDefaultProtoChunk(ChunkPos pos, BlockableEventLoop<Runnable> executor, ServerLevel level) { throw new UnsupportedOperationException(); }
}
