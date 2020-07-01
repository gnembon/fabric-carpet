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
    private LongSet pendingSkylightUpdates;

    @Shadow
    @Final
    private LongSet field_15816;

    @Shadow
    @Final
    private LongSet field_15820;

    @Shadow protected abstract boolean isAboveTopmostLightArray(final long pos);

    @Shadow protected abstract boolean isLightEnabled(final long sectionPos);

    @Override
    public void processRemoveLightData(final long cPos)
    {
        for (int y = -1; y < 17; ++y)
        {
            final long sectionPos = ChunkSectionPos.asLong(ChunkSectionPos.getX(cPos), y, ChunkSectionPos.getZ(cPos));

            this.pendingSkylightUpdates.remove(sectionPos);
            this.field_15816.remove(sectionPos);

            this.field_15820.remove(sectionPos);
        }
    }

    @Override
    public void processRelight(final ChunkLightProvider<?, ?> lightProvider, final long cPos)
    {
        final LevelPropagatorInterface levelPropagator = (LevelPropagatorInterface) lightProvider;

        for (int y = -1; y < 17; ++y)
        {
            final long sectionPos = ChunkSectionPos.asLong(ChunkSectionPos.getX(cPos), y, ChunkSectionPos.getZ(cPos));
            final long pos = BlockPos.asLong(ChunkSectionPos.getWorldCoord(ChunkSectionPos.getX(sectionPos)), ChunkSectionPos.getWorldCoord(y), ChunkSectionPos.getWorldCoord(ChunkSectionPos.getZ(sectionPos)));

            if (!this.hasLight(sectionPos))
                continue;

            for (final Direction dir : Direction.Type.HORIZONTAL)
            {
                long neighborCeilingSectionPos = ChunkSectionPos.offset(sectionPos, dir);
                final ChunkNibbleArray neighborLightArray = this.getLightArray(neighborCeilingSectionPos);

                ChunkNibbleArray neighborCeilingLightArray = neighborLightArray;

                while (neighborCeilingLightArray == null && !this.isAboveTopmostLightArray(neighborCeilingSectionPos))
                {
                    neighborCeilingSectionPos = ChunkSectionPos.offset(neighborCeilingSectionPos, Direction.UP);
                    neighborCeilingLightArray = this.getLightArray(neighborCeilingSectionPos);
                }

                final int ox = 15 * Math.max(dir.getOffsetX(), 0);
                final int oz = 15 * Math.max(dir.getOffsetZ(), 0);

                final int dx = Math.abs(dir.getOffsetZ());
                final int dz = Math.abs(dir.getOffsetX());

                for (int t = 0; t < 16; ++t)
                    for (int dy = 0; dy < 16; ++dy)
                    {
                        final long dst = BlockPos.add(pos, ox + t * dx, dy, oz + t * dz);
                        long src = BlockPos.offset(dst, dir);

                        if (neighborLightArray == null)
                            src = BlockPos.asLong(BlockPos.unpackLongX(src), ChunkSectionPos.getWorldCoord(ChunkSectionPos.getY(neighborCeilingSectionPos)), BlockPos.unpackLongZ(src));

                        final int srcLevel = neighborCeilingLightArray != null ?
                            ((ChunkLightProviderInterface) lightProvider).callGetCurrentLevelFromArray(neighborCeilingLightArray, src)
                            : this.isLightEnabled(ChunkSectionPos.withZeroZ(neighborCeilingSectionPos)) ? 0 : 15;

                        levelPropagator.invokeUpdateLevel(src, dst, levelPropagator.callGetPropagatedLevel(src, dst, srcLevel), true);
                    }
            }
        }
    }
}
