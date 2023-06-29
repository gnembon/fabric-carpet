package carpet.mixins;

import net.minecraft.world.level.lighting.DynamicGraphMinFixedPoint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(DynamicGraphMinFixedPoint.class)
public interface DynamicGraphMinFixedPoint_resetChunkInterface
{
    @Invoker("checkEdge")
    void cmInvokeUpdateLevel(long sourceId, long id, int level, boolean decrease);

    @Invoker("computeLevelFromNeighbor")
    int cmCallGetPropagatedLevel(long sourceId, long targetId, int level);
}
