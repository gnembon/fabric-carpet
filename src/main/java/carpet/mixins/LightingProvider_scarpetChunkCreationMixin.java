package carpet.mixins;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import carpet.fakes.Lighting_scarpetChunkCreationInterface;
import net.minecraft.world.level.lighting.LayerLightEngine;
import net.minecraft.world.level.lighting.LevelLightEngine;

@Mixin(LevelLightEngine.class)
public abstract class LightingProvider_scarpetChunkCreationMixin implements Lighting_scarpetChunkCreationInterface
{
    @Shadow
    @Final
    private LayerLightEngine<?, ?> blockEngine;

    @Shadow
    @Final
    private LayerLightEngine<?, ?> skyEngine;

    @Override
    public void removeLightData(final long pos)
    {
        if (this.blockEngine != null)
            ((Lighting_scarpetChunkCreationInterface) this.blockEngine).removeLightData(pos);

        if (this.skyEngine != null)
            ((Lighting_scarpetChunkCreationInterface) this.skyEngine).removeLightData(pos);
    }

    @Override
    public void relight(final long pos)
    {
        if (this.blockEngine != null)
            ((Lighting_scarpetChunkCreationInterface) this.blockEngine).relight(pos);

        if (this.skyEngine != null)
            ((Lighting_scarpetChunkCreationInterface) this.skyEngine).relight(pos);
    }
}
