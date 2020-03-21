package carpet.fakes;

import net.minecraft.util.math.ChunkPos;

import java.util.List;

public interface ThreadedAnvilChunkStorageInterface
{
    void regenerateChunkRegion(List<ChunkPos> requestedChunks);
}
