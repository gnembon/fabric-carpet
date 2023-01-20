package carpet.mixins;

import org.spongepowered.asm.mixin.Mixin;

import carpet.CarpetSettings;
import carpet.fakes.BlockBehaviourInterface;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.EndRodBlock;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(ChainBlock.class)
public class ChainBlock_customStickyMixin implements BlockBehaviourInterface {

    @Override
    public boolean isSticky(BlockState state) {
        return CarpetSettings.doChainStone;
    }

    @Override
    public boolean isStickyToNeighbor(Level level, BlockPos pos, BlockState state, BlockPos neighborPos, BlockState neighborState, Direction dir, Direction moveDir) {
        Axis axis = state.getValue(ChainBlock.AXIS);

        if (axis != dir.getAxis()) {
            return false;
        }

        if (CarpetSettings.chainStoneStickToAll) {
            return true;
        }
        if (neighborState.is((Block)(Object)this)) {
            return axis == neighborState.getValue(ChainBlock.AXIS);
        }
        if (neighborState.is(Blocks.END_ROD)) {
            return axis == neighborState.getValue(EndRodBlock.FACING).getAxis();
        }

        return Block.canSupportCenter(level, neighborPos, dir.getOpposite());
    }
}
