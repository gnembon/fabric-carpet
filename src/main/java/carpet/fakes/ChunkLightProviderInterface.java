package carpet.fakes;

import net.minecraft.world.chunk.ChunkNibbleArray;

public interface ChunkLightProviderInterface
{
    int callGetCurrentLevelFromSection(ChunkNibbleArray array, long blockPos);
}
