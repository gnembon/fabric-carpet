package carpet.fakes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface BlockBehaviourInterface {

    /**
     * @return whether this block is sticky in any way when moved by pistons
     */
    public boolean isSticky(BlockState state);

    /**
     * @return whether the neighboring block is pulled along if this block is moved by pistons
     */
    public boolean isStickyToNeighbor(Level level, BlockPos pos, BlockState state, BlockPos neighborPos, BlockState neighborState, Direction dir, Direction moveDir);

}
