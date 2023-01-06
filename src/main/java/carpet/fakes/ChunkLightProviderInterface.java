package carpet.fakes;

import net.minecraft.world.level.chunk.DataLayer;

public interface ChunkLightProviderInterface
{
    int callGetCurrentLevelFromSection(DataLayer array, long blockPos);
}
