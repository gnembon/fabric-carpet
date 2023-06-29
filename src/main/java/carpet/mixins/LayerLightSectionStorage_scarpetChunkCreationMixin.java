package carpet.mixins;

import java.util.Arrays;

import carpet.fakes.Lighting_scarpetChunkCreationInterface;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.DataLayerStorageMap;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongSet;

@Mixin(LayerLightSectionStorage.class)
public abstract class LayerLightSectionStorage_scarpetChunkCreationMixin implements Lighting_scarpetChunkCreationInterface
{
    @Shadow
    protected abstract DataLayer getDataLayer(long sectionPos, boolean cached);

    @Shadow
    @Final
    protected LongSet changedSections;

    @Shadow
    @Final
    protected DataLayerStorageMap<?> updatingSectionData;

    @Shadow protected abstract boolean storingLightForSection(long sectionPos);

    @Shadow
    @Final
    protected Long2ObjectMap<DataLayer> queuedSections;

    @Override
    public void removeLightData(long cPos)
    {

        for (int y = -1; y < 17; ++y)
        {
            long sectionPos = SectionPos.asLong(SectionPos.x(cPos), y, SectionPos.z(cPos));

            this.queuedSections.remove(sectionPos);

            if (this.storingLightForSection(sectionPos))
            {
                if (this.changedSections.add(sectionPos))
                    this.updatingSectionData.copyDataLayer(sectionPos);

                Arrays.fill(this.getDataLayer(sectionPos, true).getData(), (byte) 0);
            }
        }
    }
}
