package carpet.fakes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface BlockBehaviourInterface {

    /**
     * Use {@link BlockPistonBehaviourInterface#isSticky(BlockState)} instead
     */
    @Deprecated(forRemoval = true)
    default boolean isSticky(BlockState state){
        return false;
    }

    /**
     * Use {@link BlockPistonBehaviourInterface#isStickyToNeighbor(Level, BlockPos, BlockState, BlockPos, BlockState, Direction, Direction)} instead
     */
    @Deprecated(forRemoval = true)
    default boolean isStickyToNeighbor(Level level, BlockPos pos, BlockState state, BlockPos neighborPos, BlockState neighborState, Direction dir, Direction moveDir){
        return false;
    }
}
