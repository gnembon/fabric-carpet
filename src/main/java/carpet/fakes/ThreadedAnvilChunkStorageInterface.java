package carpet.fakes;

import java.util.List;
import java.util.Map;

import net.minecraft.util.math.ChunkPos;

public interface ThreadedAnvilChunkStorageInterface
{
    Map<String, Integer> regenerateChunkRegion(List<ChunkPos> requestedChunks);

    void releaseRelightTicket(ChunkPos pos);
}
