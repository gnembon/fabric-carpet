package carpet.mixins;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import carpet.fakes.ChunkLightProviderInterface;
import carpet.fakes.LightStorageInterface;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkProvider;
import net.minecraft.world.chunk.light.BlockLightStorage.Data;
import net.minecraft.world.chunk.light.ChunkLightProvider;
import net.minecraft.world.chunk.light.LightStorage;
import net.minecraft.world.chunk.light.SkyLightStorage;

@Mixin(SkyLightStorage.class)
public abstract class SkyLightStorage_scarpetChunkCreationMixin extends LightStorage<Data> implements LightStorageInterface
{
    protected SkyLightStorage_scarpetChunkCreationMixin(final LightType lightType, final ChunkProvider chunkProvider, final Data lightData)
    {
        super(lightType, chunkProvider, lightData);
    }

    @Shadow
    @Final
    private LongSet sectionsToUpdate;

    @Shadow
    @Final
    private LongSet sectionsToRemove;

    @Shadow
    @Final
    private LongSet field_15820;

    @Shadow protected abstract boolean isAtOrAboveTopmostSection(final long pos);

    @Shadow protected abstract boolean isSectionEnabled(final long sectionPos);

    @Override
    public void processRemoveLightData(final long cPos)
    {
        for (int y = -1; y < 17; ++y)
        {
            final long sectionPos = ChunkSectionPos.asLong(ChunkSectionPos.unpackX(cPos), y, ChunkSectionPos.unpackZ(cPos));

            this.sectionsToUpdate.remove(sectionPos);
            this.sectionsToRemove.remove(sectionPos);

            this.field_15820.remove(sectionPos);
        }
    }

    @Override
    public void processRelight(final ChunkLightProvider<?, ?> lightProvider, final long cPos)
    {
        final LevelPropagator_resetChunkInterface levelPropagator = (LevelPropagator_resetChunkInterface) lightProvider;

        for (int y = -1; y < 17; ++y)
        {
            final long sectionPos = ChunkSectionPos.asLong(ChunkSectionPos.unpackX(cPos), y, ChunkSectionPos.unpackZ(cPos));
            final long pos = BlockPos.asLong(ChunkSectionPos.getBlockCoord(ChunkSectionPos.unpackX(sectionPos)), ChunkSectionPos.getBlockCoord(y), ChunkSectionPos.getBlockCoord(ChunkSectionPos.unpackZ(sectionPos)));

            if (!this.hasSection(sectionPos))
                continue;

            for (final Direction dir : Direction.Type.HORIZONTAL)
            {
                long neighborCeilingSectionPos = ChunkSectionPos.offset(sectionPos, dir);
                final ChunkNibbleArray neighborLightArray = this.getLightSection(neighborCeilingSectionPos);

                ChunkNibbleArray neighborCeilingLightArray = neighborLightArray;

                while (neighborCeilingLightArray == null && !this.isAtOrAboveTopmostSection(neighborCeilingSectionPos))
                {
                    neighborCeilingSectionPos = ChunkSectionPos.offset(neighborCeilingSectionPos, Direction.UP);
                    neighborCeilingLightArray = this.getLightSection(neighborCeilingSectionPos);
                }

                final int ox = 15 * Math.max(dir.getOffsetX(), 0);
                final int oz = 15 * Math.max(dir.getOffsetZ(), 0);

                final int dx = Math.abs(dir.getOffsetZ());
                final int dz = Math.abs(dir.getOffsetX());

                int emptyLightLevel = (neighborCeilingLightArray == null)
                    ? (this.isSectionEnabled(ChunkSectionPos.withZeroY(neighborCeilingSectionPos)) ? 0 : 15)
                    : 0;
                int neighbourY = (neighborLightArray == null)
                    ? ChunkSectionPos.getBlockCoord(ChunkSectionPos.unpackY(neighborCeilingSectionPos))
                    : 0;

                for (int t = 0; t < 16; ++t)
                    for (int dy = 0; dy < 16; ++dy)
                    {
                        final long dst = BlockPos.add(pos, ox + t * dx, dy, oz + t * dz);
                        long src = BlockPos.offset(dst, dir);

                        long adj_src = (neighborLightArray != null)
                            ? src
                            : BlockPos.asLong(BlockPos.unpackLongX(src), neighbourY, BlockPos.unpackLongZ(src));

                        final int srcLevel = neighborCeilingLightArray != null
                            ? ((ChunkLightProviderInterface) lightProvider).callGetCurrentLevelFromSection(neighborCeilingLightArray, adj_src)
                            : emptyLightLevel;

                        levelPropagator.cmInvokeUpdateLevel(src, dst, levelPropagator.cmCallGetPropagatedLevel(src, dst, srcLevel), true);
                    }
            }
        }
    }
}
