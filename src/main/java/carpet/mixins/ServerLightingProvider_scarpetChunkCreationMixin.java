package carpet.mixins;

import java.util.concurrent.CompletableFuture;
import java.util.function.IntSupplier;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;

import carpet.fakes.Lighting_scarpetChunkCreationInterface;
import carpet.fakes.ServerLightingProviderInterface;
import carpet.fakes.ThreadedAnvilChunkStorageInterface;
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.server.world.ServerLightingProvider.Stage;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.Util;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkProvider;
import net.minecraft.world.chunk.light.LightingProvider;

@Mixin(ServerLightingProvider.class)
public abstract class ServerLightingProvider_scarpetChunkCreationMixin extends LightingProvider implements ServerLightingProviderInterface
{
    private ServerLightingProvider_scarpetChunkCreationMixin(final ChunkProvider chunkProvider, final boolean hasBlockLight, final boolean hasSkyLight)
    {
        super(chunkProvider, hasBlockLight, hasSkyLight);
    }

    @Shadow
    protected abstract void enqueue(final int x, final int z, final IntSupplier completedLevelSupplier, final Stage stage, final Runnable task);

    @Shadow
    @Final
    private ThreadedAnvilChunkStorage chunkStorage;

    @Override
    @Invoker("updateChunkStatus")
    public abstract void invokeUpdateChunkStatus(ChunkPos pos);

    @Override
    public void removeLightData(final Chunk chunk)
    {
        final ChunkPos pos = chunk.getPos();
        chunk.setLightOn(false);

        this.enqueue(pos.x, pos.z, () -> 0, ServerLightingProvider.Stage.PRE_UPDATE, Util.debugRunnable(() -> {
                super.setLightEnabled(pos, false);
                ((Lighting_scarpetChunkCreationInterface) this).removeLightData(ChunkSectionPos.withZeroZ(ChunkSectionPos.asLong(pos.x, 0, pos.z)));
            },
            () -> "Remove light data " + pos
        ));
    }

    @Override
    public CompletableFuture<Void> relight(final Chunk chunk)
    {
        final ChunkPos pos = chunk.getPos();

        this.enqueue(pos.x, pos.z, () -> 0, ServerLightingProvider.Stage.PRE_UPDATE, Util.debugRunnable(() -> {
                super.setLightEnabled(pos, true);

                chunk.getLightSourcesStream().forEach(
                    blockPos -> super.addLightSource(blockPos, chunk.getLuminance(blockPos))
                );

                ((Lighting_scarpetChunkCreationInterface) this).relight(ChunkSectionPos.withZeroZ(ChunkSectionPos.asLong(pos.x, 0, pos.z)));
            },
            () -> "Relight chunk " + pos
        ));

        return CompletableFuture.runAsync(
            Util.debugRunnable(() -> {
                    chunk.setLightOn(true);
                    ((ThreadedAnvilChunkStorageInterface) this.chunkStorage).releaseRelightTicket(pos);
                },
                () -> "Release relight ticket " + pos
            ),
            runnable -> this.enqueue(pos.x, pos.z, () -> 0, ServerLightingProvider.Stage.POST_UPDATE, runnable)
        );
    }
}
