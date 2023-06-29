package carpet.fakes;

import java.util.concurrent.CompletableFuture;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;

public interface ServerLightingProviderInterface
{
    void invokeUpdateChunkStatus(ChunkPos pos);

    void removeLightData(ChunkAccess chunk);

    CompletableFuture<Void> relight(ChunkAccess chunk);

    void resetLight(ChunkAccess chunk, ChunkPos pos);
}
