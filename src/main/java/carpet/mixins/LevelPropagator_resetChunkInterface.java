package carpet.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.chunk.light.LevelPropagator;

@Mixin(LevelPropagator.class)
public interface LevelPropagator_resetChunkInterface
{
    @Invoker("updateLevel")
    void cmInvokeUpdateLevel(long sourceId, long id, int level, boolean decrease);

    @Invoker("getPropagatedLevel")
    int cmCallGetPropagatedLevel(long sourceId, long targetId, int level);
}
