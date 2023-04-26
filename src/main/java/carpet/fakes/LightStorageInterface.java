package carpet.fakes;

import net.minecraft.world.level.lighting.LightEngine;

public interface LightStorageInterface extends Lighting_scarpetChunkCreationInterface
{
    void processRemoveLightData(long pos);

    void processRelight(LightEngine<?, ?> lightProvider, long pos);

    int getLightLevelByLong(long blockPos);
}
