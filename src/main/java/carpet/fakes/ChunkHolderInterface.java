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
    CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> setDefaultProtoChunk(ChunkPos chpos, BlockableEventLoop<Runnable> executor, ServerLevel world);
}
