package carpet.fakes;

import net.minecraft.util.math.ChunkPos;

public interface ServerLightingProviderInterface
{
    void publicUpdateChunkStatus(ChunkPos pos);
    void flush();
}
