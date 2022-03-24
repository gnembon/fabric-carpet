package carpet.mixins;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;

import carpet.fakes.ChunkLightProviderInterface;
import carpet.fakes.Lighting_scarpetChunkCreationInterface;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.LayerLightEngine;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;

@Mixin(LayerLightEngine.class)
public abstract class LayerLightEngine_scarpetChunkCreationMixin implements Lighting_scarpetChunkCreationInterface, ChunkLightProviderInterface
{
    @Shadow
    @Final
    protected LayerLightSectionStorage<?> storage;

    @Override
    public void removeLightData(final long pos)
    {
        ((Lighting_scarpetChunkCreationInterface) this.storage).removeLightData(pos);
    }

    @Override
    public void relight(final long pos)
    {
        ((Lighting_scarpetChunkCreationInterface) this.storage).relight(pos);
    }

    @Override
    @Invoker("getLevel")
    public abstract int callGetCurrentLevelFromSection(DataLayer array, long blockPos);
}
