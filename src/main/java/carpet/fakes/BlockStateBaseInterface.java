package carpet.fakes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface BlockStateBaseInterface {

    public boolean isSticky();

    public boolean isStickyToNeighbor(Level level, BlockPos pos, BlockPos neighborPos, BlockState neighborState, Direction dir, Direction moveDir);

}
