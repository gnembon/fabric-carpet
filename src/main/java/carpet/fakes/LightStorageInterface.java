package carpet.fakes;

import net.minecraft.world.chunk.light.ChunkLightProvider;

public interface LightStorageInterface extends Lighting_scarpetChunkCreationInterface
{
    void processRemoveLightData(long pos);

    void processRelight(ChunkLightProvider<?, ?> lightProvider, long pos);
}
