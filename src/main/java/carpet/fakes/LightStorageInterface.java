package carpet.fakes;

import net.minecraft.world.level.lighting.LayerLightEngine;

public interface LightStorageInterface extends Lighting_scarpetChunkCreationInterface
{
    void processRemoveLightData(long pos);

    void processRelight(LayerLightEngine<?, ?> lightProvider, long pos);
}
