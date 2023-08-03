package carpet.fakes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface RedStoneWireBlockInterface
{
    default BlockState carpet$updateLogicPublic(Level level, BlockPos pos, BlockState state) { throw new UnsupportedOperationException(); }

    default void carpet$setShouldSignal(boolean shouldSignal) { throw new UnsupportedOperationException(); }

    default boolean carpet$getShouldSignal() { throw new UnsupportedOperationException(); }
}
