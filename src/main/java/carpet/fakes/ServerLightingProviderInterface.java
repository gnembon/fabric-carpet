package carpet.fakes;

import java.util.concurrent.CompletableFuture;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;

public interface ServerLightingProviderInterface
{
    void invokeUpdateChunkStatus(ChunkPos pos);

    void removeLightData(Chunk chunk);

    CompletableFuture<Void> relight(Chunk chunk);

    void resetLight(Chunk chunk, ChunkPos pos);
}
