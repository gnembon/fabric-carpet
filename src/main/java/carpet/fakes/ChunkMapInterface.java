package carpet.fakes;

import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.world.level.ChunkPos;

public interface ChunkMapInterface
{
    default Map<String, Integer> carpet$regenerateChunkRegion(List<ChunkPos> requestedChunks) { throw new UnsupportedOperationException(); }

    default void carpet$relightChunk(ChunkPos pos) { throw new UnsupportedOperationException(); }

    default void carpet$releaseRelightTicket(ChunkPos pos) { throw new UnsupportedOperationException(); }

    default Iterable<ChunkHolder> carpet$getChunks() { throw new UnsupportedOperationException(); }
}
