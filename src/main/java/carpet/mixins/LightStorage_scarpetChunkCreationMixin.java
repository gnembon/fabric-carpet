package carpet.mixins;

import java.util.Arrays;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.DataLayerStorageMap;
import net.minecraft.world.level.lighting.LayerLightEngine;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import carpet.fakes.LightStorageInterface;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

@Mixin(LayerLightSectionStorage.class)
public abstract class LightStorage_scarpetChunkCreationMixin implements LightStorageInterface
{
    @Shadow
    protected abstract DataLayer getDataLayer(final long sectionPos, final boolean cached);

    @Shadow
    protected abstract void clearQueuedSectionBlocks(final LayerLightEngine<?, ?> storage, final long blockChunkPos);

    @Shadow
    @Final
    protected LongSet changedSections;

    @Shadow
    @Final
    protected DataLayerStorageMap<?> updatingSectionData;

    @Shadow protected abstract boolean storingLightForSection(final long sectionPos);

    @Shadow
    @Final
    protected Long2ObjectMap<DataLayer> queuedSections;

    @Shadow protected abstract void enableLightSources(final long l, final boolean bl);

    @Unique
    private final LongSet removedChunks = new LongOpenHashSet();

    @Unique
    private final LongSet relightChunks = new LongOpenHashSet();

    @Override
    public void removeLightData(final long pos)
    {
        this.removedChunks.add(pos);
    }

    @Override
    public void relight(final long pos)
    {
        this.relightChunks.add(pos);
    }

    @Inject(
        method = "markNewInconsistencies(Lnet/minecraft/world/level/lighting/LayerLightEngine;ZZ)V",
        at = @At("HEAD")
    )
    private void processData(final LayerLightEngine<?, ?> lightProvider, final boolean doSkylight, final boolean skipEdgeLightPropagation, final CallbackInfo ci)
    {
        // Process light removal

        for (final long cPos : this.removedChunks)
        {
            for (int y = -1; y < 17; ++y)
            {
                final long sectionPos = SectionPos.asLong(SectionPos.x(cPos), y, SectionPos.z(cPos));

                this.queuedSections.remove(sectionPos);

                if (this.storingLightForSection(sectionPos))
                {
                    this.clearQueuedSectionBlocks(lightProvider, sectionPos);

                    if (this.changedSections.add(sectionPos))
                        this.updatingSectionData.copyDataLayer(sectionPos);

                    Arrays.fill(this.getDataLayer(sectionPos, true).getData(), (byte) 0);
                }
            }

            this.processRemoveLightData(cPos);
        }

        this.removedChunks.clear();

        // Process relighting

        for (final long cPos : this.relightChunks)
            this.processRelight(lightProvider, cPos);

        this.relightChunks.clear();
    }

    @Override
    public void processRemoveLightData(final long pos)
    {
    }

    @Override
    public void processRelight(final LayerLightEngine<?, ?> lightProvider, final long pos)
    {
    }
}
