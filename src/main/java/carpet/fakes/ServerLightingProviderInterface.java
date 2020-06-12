package carpet.fakes;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;

public interface ServerLightingProviderInterface
{
    void publicUpdateChunkStatus(ChunkPos pos);
    void flush();
    void resetLight(Chunk chunk, ChunkPos pos);
}
