package carpet.mixins;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;

import carpet.fakes.ChunkLightProviderInterface;
import carpet.fakes.Lighting_scarpetChunkCreationInterface;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.light.ChunkLightProvider;
import net.minecraft.world.chunk.light.LightStorage;

@Mixin(ChunkLightProvider.class)
public abstract class ChunkLightProvider_scarpetChunkCreationMixin implements Lighting_scarpetChunkCreationInterface, ChunkLightProviderInterface
{
    @Shadow
    @Final
    protected LightStorage<?> lightStorage;

    @Override
    public void removeLightData(final long pos)
    {
        ((Lighting_scarpetChunkCreationInterface) this.lightStorage).removeLightData(pos);
    }

    @Override
    public void relight(final long pos)
    {
        ((Lighting_scarpetChunkCreationInterface) this.lightStorage).relight(pos);
    }

    @Override
    @Invoker("getCurrentLevelFromArray")
    public abstract int callGetCurrentLevelFromArray(ChunkNibbleArray array, long blockPos);
}
