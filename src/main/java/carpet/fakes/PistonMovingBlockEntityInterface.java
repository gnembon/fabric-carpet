package carpet.fakes;

import net.minecraft.world.level.block.entity.BlockEntity;

public interface PistonMovingBlockEntityInterface
{
    default void carpet$setCarriedBlockEntity(BlockEntity blockEntity) { throw new UnsupportedOperationException(); }

    default BlockEntity carpet$getCarriedBlockEntity() { throw new UnsupportedOperationException(); }

    default void carpet$setRenderCarriedBlockEntity(boolean b) { throw new UnsupportedOperationException(); }

    default boolean carpet$getRenderCarriedBlockEntity() { throw new UnsupportedOperationException(); }

    default boolean carpet$isRenderModeSet() { throw new UnsupportedOperationException(); }
}
