package carpet.mixins;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import carpet.fakes.Lighting_scarpetChunkCreationInterface;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.lighting.LevelLightEngine;

@Mixin(LevelLightEngine.class)
public abstract class LevelLightEngine_scarpetChunkCreationMixin implements Lighting_scarpetChunkCreationInterface
{
    @Shadow
    @Final
    private LightEngine<?, ?> blockEngine;

    @Shadow
    @Final
    private LightEngine<?, ?> skyEngine;

    @Override
    public void carpet$removeLightData(final long pos)
    {
        if (this.blockEngine != null)
            this.blockEngine.carpet$removeLightData(pos);

        if (this.skyEngine != null)
            this.skyEngine.carpet$removeLightData(pos);
    }
}
