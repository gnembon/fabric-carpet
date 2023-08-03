package carpet.fakes;

import net.minecraft.core.BlockPos;

public interface BlockEntityInterface
{
    default void carpet$setPos(BlockPos pos) { throw new UnsupportedOperationException(); }
}
