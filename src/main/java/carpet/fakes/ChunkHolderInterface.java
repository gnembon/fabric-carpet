package carpet.fakes;

import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;

public interface ChunkHolderInterface
{
    //CompletableFuture<ChunkResult<ChunkAccess>> setDefaultProtoChunk(ChunkPos chpos, BlockableEventLoop<Runnable> executor, ServerLevel world);
}
