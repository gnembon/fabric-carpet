package carpet.fakes;

import net.minecraft.util.math.ChunkPos;

public interface ThreadedAnvilChunkStorageInterface
{
    //boolean isChunkLoaded(ChunkPos chpos);
    //void forceUnloadChunk(ChunkPos chpos);
    void regenerateChunk(ChunkPos chpos);
}
