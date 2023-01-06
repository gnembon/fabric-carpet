package carpet.mixins;

import carpet.fakes.ServerLightingProviderInterface;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ThreadedLevelLightEngine.class)
public abstract class ThreadedLevelLightEngine_scarpetMixin extends LevelLightEngine implements ServerLightingProviderInterface
{
    @Shadow public abstract void checkBlock(BlockPos pos);

    public ThreadedLevelLightEngine_scarpetMixin(LightChunkGetter chunkProvider, boolean hasBlockLight, boolean hasSkyLight)
    {
        super(chunkProvider, hasBlockLight, hasSkyLight);
    }

    @Override
    public void resetLight(ChunkAccess chunk, ChunkPos pos)
    {
        //super.setRetainData(pos, false);
        //super.setLightEnabled(pos, false);
        //for (int x = chpos.x-1; x <= chpos.x+1; x++ )
        //    for (int z = chpos.z-1; z <= chpos.z+1; z++ )
            {
                //ChunkPos pos = new ChunkPos(x, z);
                int j;
                for(j = -1; j < 17; ++j) {                                                                 // skip some recomp
                    super.queueSectionData(LightLayer.BLOCK, SectionPos.of(pos, j), new DataLayer(), false);
                    super.queueSectionData(LightLayer.SKY, SectionPos.of(pos, j), new DataLayer(), false);
                }
                for(j = 0; j < 16; ++j) {
                    super.updateSectionStatus(SectionPos.of(pos, j), true);
                }

                super.enableLightSources(pos, true);

                    chunk.getLights().forEach((blockPos) -> {
                        super.onBlockEmissionIncrease(blockPos, chunk.getLightEmission(blockPos));
                    });

            }




    }
}
