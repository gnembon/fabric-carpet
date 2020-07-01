package carpet.mixins;

import org.spongepowered.asm.mixin.Mixin;

import carpet.fakes.ChunkLightProviderInterface;
import carpet.fakes.LightStorageInterface;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkProvider;
import net.minecraft.world.chunk.light.BlockLightStorage;
import net.minecraft.world.chunk.light.BlockLightStorage.Data;
import net.minecraft.world.chunk.light.ChunkLightProvider;
import net.minecraft.world.chunk.light.LightStorage;

@Mixin(BlockLightStorage.class)
public abstract class BlockLightStorage_scarpetChunkCreationMixin extends LightStorage<Data> implements LightStorageInterface
{
    private BlockLightStorage_scarpetChunkCreationMixin(final LightType lightType, final ChunkProvider chunkProvider, final Data lightData)
    {
        super(lightType, chunkProvider, lightData);
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
                final ChunkNibbleArray neighborLightArray = this.getLightArray(ChunkSectionPos.offset(sectionPos, dir));

                if (neighborLightArray == null)
                    continue;

                final int ox = 15 * Math.max(dir.getOffsetX(), 0);
                final int oz = 15 * Math.max(dir.getOffsetZ(), 0);

                final int dx = Math.abs(dir.getOffsetZ());
                final int dz = Math.abs(dir.getOffsetX());

                for (int t = 0; t < 16; ++t)
                    for (int dy = 0; dy < 16; ++dy)
                    {
                        final long dst = BlockPos.add(pos, ox + t * dx, dy, oz + t * dz);
                        final long src = BlockPos.offset(dst, dir);

                        final int srcLevel = ((ChunkLightProviderInterface) lightProvider).callGetCurrentLevelFromArray(neighborLightArray, src);

                        levelPropagator.invokeUpdateLevel(src, dst, levelPropagator.callGetPropagatedLevel(src, dst, srcLevel), true);
                    }
            }
        }
    }
}
