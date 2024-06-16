package carpet.mixins;

import java.util.concurrent.CompletableFuture;
import java.util.function.IntSupplier;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.server.level.ThreadedLevelLightEngine.TaskType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;

import carpet.fakes.Lighting_scarpetChunkCreationInterface;
import carpet.fakes.ServerLightingProviderInterface;
import carpet.fakes.ThreadedAnvilChunkStorageInterface;

@Mixin(ThreadedLevelLightEngine.class)
public abstract class ThreadedLevelLightEngine_scarpetChunkCreationMixin extends LevelLightEngine implements ServerLightingProviderInterface
{
    private ThreadedLevelLightEngine_scarpetChunkCreationMixin(final LightChunkGetter chunkProvider, final boolean hasBlockLight, final boolean hasSkyLight)
    {
        super(chunkProvider, hasBlockLight, hasSkyLight);
    }

    @Shadow
    protected abstract void addTask(final int x, final int z, final IntSupplier completedLevelSupplier, final TaskType stage, final Runnable task);

    @Shadow
    @Final
    private ChunkMap chunkMap;

    @Override
    @Invoker("updateChunkStatus")
    public abstract void invokeUpdateChunkStatus(ChunkPos pos);

    @Override
    public void removeLightData(final ChunkAccess chunk)
    {
        ChunkPos pos = chunk.getPos();
        chunk.setLightCorrect(false);

        this.addTask(pos.x, pos.z, () -> 0, ThreadedLevelLightEngine.TaskType.PRE_UPDATE, Util.name(() -> {
                super.setLightEnabled(pos, false);
                ((Lighting_scarpetChunkCreationInterface) this).removeLightData(SectionPos.getZeroNode(SectionPos.asLong(pos.x, 0, pos.z)));
            },
            () -> "Remove light data " + pos
        ));
    }

    @Override
    public CompletableFuture<Void> relight(ChunkAccess chunk)
    {
        ChunkPos pos = chunk.getPos();

        this.addTask(pos.x, pos.z, () -> 0, ThreadedLevelLightEngine.TaskType.PRE_UPDATE, Util.name(() -> {
                super.propagateLightSources(pos);
                int minY = chunk.getMinBuildHeight();
                int maxY = chunk.getMaxBuildHeight();
                int minX = pos.getMinBlockX();
                int minZ = pos.getMinBlockZ();
                BlockPos.MutableBlockPos poss = new BlockPos.MutableBlockPos();
                for (int x = -1; x < 17; ++x)
                {
                    for (int z = -1; z < 17; ++z)
                    {
                        if (x > 0 && x < 16 && z > 0 && z < 16)
                        {// not really efficient way to do it, but hey, we have bigger problems with this
                            continue;
                        }
                        for (int y = minY; y < maxY; ++y)
                        {
                            poss.set(x + minX, y, z + minZ);
                            super.checkBlock(poss);
                        }
                    }
                }
            },
            () -> "Relight chunk " + pos
        ));

        return CompletableFuture.runAsync(
            Util.name(() -> {
                    chunk.setLightCorrect(true);
                    //((ThreadedAnvilChunkStorageInterface) this.chunkMap).releaseRelightTicket(pos);
                },
                () -> "Release relight ticket " + pos
            ),
            runnable -> this.addTask(pos.x, pos.z, () -> 0, ThreadedLevelLightEngine.TaskType.POST_UPDATE, runnable)
        );
    }
}
