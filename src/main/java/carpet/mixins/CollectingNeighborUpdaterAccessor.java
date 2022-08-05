package carpet.mixins;

import net.minecraft.world.level.redstone.CollectingNeighborUpdater;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CollectingNeighborUpdater.class)
public interface CollectingNeighborUpdaterAccessor {
    @Accessor("count")
    void setCount(int count);
    @Accessor("maxChainedNeighborUpdates")
    int getMaxChainedNeighborUpdates();
}
