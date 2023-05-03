package carpet.mixins;

import carpet.fakes.LightStorageInterface;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import carpet.fakes.ChunkLightProviderInterface;
import carpet.fakes.Lighting_scarpetChunkCreationInterface;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;

@Mixin(LightEngine.class)
public abstract class LayerLightEngine_scarpetChunkCreationMixin implements Lighting_scarpetChunkCreationInterface, ChunkLightProviderInterface
{
    @Shadow
    @Final
    protected LayerLightSectionStorage<?> storage;

    //@Shadow protected abstract void clearQueuedSectionBlocks(final long l);

    @Override
    public void removeLightData(final long pos)
    {
        ((Lighting_scarpetChunkCreationInterface) this.storage).removeLightData(pos);
    }

    @Override
    public void relight(final long pos)
    {
        ((Lighting_scarpetChunkCreationInterface) this.storage).relight(pos);
    }

    @Override
    public int callGetCurrentLevelFromSection(DataLayer array, long blockPos) {
        return ((LightStorageInterface)storage).getLightLevelByLong(blockPos);
    };

    @Override
    public void clearQueuedSectionBlocksPublicAccess(long sectionPos)
    {
        //clearQueuedSectionBlocks(sectionPos);
    }
}
