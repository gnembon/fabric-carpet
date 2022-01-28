package carpet.mixins;

import org.spongepowered.asm.mixin.Mixin;

import carpet.fakes.ChunkLightProviderInterface;
import carpet.fakes.LightStorageInterface;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.BlockLightSectionStorage;
import net.minecraft.world.level.lighting.BlockLightSectionStorage.BlockDataLayerStorageMap;
import net.minecraft.world.level.lighting.LayerLightEngine;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;

@Mixin(BlockLightSectionStorage.class)
public abstract class BlockLightSectionStorage_scarpetChunkCreationMixin extends LayerLightSectionStorage<BlockDataLayerStorageMap> implements LightStorageInterface
{
    private BlockLightSectionStorage_scarpetChunkCreationMixin(final LightLayer lightType, final LightChunkGetter chunkProvider, final BlockDataLayerStorageMap lightData)
    {
        super(lightType, chunkProvider, lightData);
    }

    @Override
    public void processRelight(final LayerLightEngine<?, ?> lightProvider, final long cPos)
    {
        final DynamicGraphMinFixedPoint_resetChunkInterface levelPropagator = (DynamicGraphMinFixedPoint_resetChunkInterface) lightProvider;

        for (int y = -1; y < 17; ++y)
        {
            final long sectionPos = SectionPos.asLong(SectionPos.x(cPos), y, SectionPos.z(cPos));
            final long pos = BlockPos.asLong(SectionPos.sectionToBlockCoord(SectionPos.x(sectionPos)), SectionPos.sectionToBlockCoord(y), SectionPos.sectionToBlockCoord(SectionPos.z(sectionPos)));

            if (!this.storingLightForSection(sectionPos))
                continue;

            for (final Direction dir : Direction.Plane.HORIZONTAL)
            {
                final DataLayer neighborLightArray = this.getDataLayerData(SectionPos.offset(sectionPos, dir));

                if (neighborLightArray == null)
                    continue;

                final int ox = 15 * Math.max(dir.getStepX(), 0);
                final int oz = 15 * Math.max(dir.getStepZ(), 0);

                final int dx = Math.abs(dir.getStepZ());
                final int dz = Math.abs(dir.getStepX());

                for (int t = 0; t < 16; ++t)
                    for (int dy = 0; dy < 16; ++dy)
                    {
                        final long dst = BlockPos.offset(pos, ox + t * dx, dy, oz + t * dz);
                        final long src = BlockPos.offset(dst, dir);

                        final int srcLevel = ((ChunkLightProviderInterface) lightProvider).callGetCurrentLevelFromSection(neighborLightArray, src);

                        levelPropagator.cmInvokeUpdateLevel(src, dst, levelPropagator.cmCallGetPropagatedLevel(src, dst, srcLevel), true);
                    }
            }
        }
    }
}
