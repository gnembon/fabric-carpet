package carpet.fakes;

import java.util.concurrent.CompletableFuture;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;

public interface ServerLightingProviderInterface
{
    default void carpet$updateChunkStatus(ChunkPos pos) { throw new UnsupportedOperationException(); }

    default void carpet$removeLightData(ChunkAccess chunk) { throw new UnsupportedOperationException(); }

    default CompletableFuture<Void> carpet$relight(ChunkAccess chunk) { throw new UnsupportedOperationException(); }

    default void carpet$resetLight(ChunkAccess chunk, ChunkPos pos) { throw new UnsupportedOperationException(); }
}
