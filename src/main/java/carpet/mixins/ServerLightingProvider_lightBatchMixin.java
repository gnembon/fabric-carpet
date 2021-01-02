package carpet.mixins;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import carpet.fakes.ServerLightingProviderInterface;
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.world.chunk.ChunkProvider;
import net.minecraft.world.chunk.light.LightingProvider;

@Mixin(ServerLightingProvider.class)
public abstract class ServerLightingProvider_lightBatchMixin extends LightingProvider implements ServerLightingProviderInterface
{
    private ServerLightingProvider_lightBatchMixin(final ChunkProvider chunkProvider, final boolean hasBlockLight, final boolean hasSkyLight)
    {
        super(chunkProvider, hasBlockLight, hasSkyLight);
    }
    
    @Shadow @Final volatile int taskBatchSize;

    @Override
    public int getTaskBatchSize()
    {
        return taskBatchSize;
    }
}
