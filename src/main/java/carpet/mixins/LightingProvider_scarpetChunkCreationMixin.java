package carpet.mixins;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import carpet.fakes.Lighting_scarpetChunkCreationInterface;
import net.minecraft.world.chunk.light.ChunkLightProvider;
import net.minecraft.world.chunk.light.LightingProvider;

@Mixin(LightingProvider.class)
public abstract class LightingProvider_scarpetChunkCreationMixin implements Lighting_scarpetChunkCreationInterface
{
    @Shadow
    @Final
    private ChunkLightProvider<?, ?> blockLightProvider;

    @Shadow
    @Final
    private ChunkLightProvider<?, ?> skyLightProvider;

    @Override
    public void removeLightData(final long pos)
    {
        if (this.blockLightProvider != null)
            ((Lighting_scarpetChunkCreationInterface) this.blockLightProvider).removeLightData(pos);

        if (this.skyLightProvider != null)
            ((Lighting_scarpetChunkCreationInterface) this.skyLightProvider).removeLightData(pos);
    }

    @Override
    public void relight(final long pos)
    {
        if (this.blockLightProvider != null)
            ((Lighting_scarpetChunkCreationInterface) this.blockLightProvider).relight(pos);

        if (this.skyLightProvider != null)
            ((Lighting_scarpetChunkCreationInterface) this.skyLightProvider).relight(pos);
    }
}
