package carpet.mixins;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import carpet.fakes.Lighting_scarpetChunkCreationInterface;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;

@Mixin(LightEngine.class)
public abstract class LayerLightEngine_scarpetChunkCreationMixin implements Lighting_scarpetChunkCreationInterface
{
    @Shadow
    @Final
    protected LayerLightSectionStorage<?> storage;

    @Override
    public void removeLightData(final long pos)
    {
        ((Lighting_scarpetChunkCreationInterface) this.storage).removeLightData(pos);
    }
}
