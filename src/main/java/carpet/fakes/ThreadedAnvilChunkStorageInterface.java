package carpet.fakes;

import net.minecraft.util.math.ChunkPos;

import java.util.List;
import java.util.Map;

public interface ThreadedAnvilChunkStorageInterface
{
    Map<String, Integer> regenerateChunkRegion(List<ChunkPos> requestedChunks);
}
