package carpet.fakes;

import carpet.mixins.PistonStructureResolver_customStickyMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Opt-in Interface that allows for more control on a blocks interaction within the {@link PistonStructureResolver} via {@link PistonStructureResolver_customStickyMixin}
 */
public interface BlockPistonBehaviourInterface {

    /**
     * @return whether this block is sticky in any way when moved by pistons
     */
    boolean isSticky(BlockState state);

    /**
     * @return whether the neighboring block is pulled along if this block is moved by pistons
     */
    boolean isStickyToNeighbor(Level level, BlockPos pos, BlockState state, BlockPos neighborPos, BlockState neighborState, Direction dir, Direction moveDir);
}
