package carpet.fakes;

import net.minecraft.world.chunk.ChunkNibbleArray;

public interface ChunkLightProviderInterface
{
    int callGetCurrentLevelFromArray(ChunkNibbleArray array, long blockPos);
}
