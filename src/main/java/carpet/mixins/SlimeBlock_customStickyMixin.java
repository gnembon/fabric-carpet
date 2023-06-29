package carpet.mixins;

import org.spongepowered.asm.mixin.Mixin;

import carpet.fakes.BlockBehaviourInterface;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(SlimeBlock.class)
public class SlimeBlock_customStickyMixin implements BlockBehaviourInterface {

    @Override
    public boolean isSticky(BlockState state) {
        return true;
    }

    @Override
    public boolean isStickyToNeighbor(Level level, BlockPos pos, BlockState state, BlockPos neighborPos, BlockState neighborState, Direction dir, Direction moveDir) {
        return !neighborState.is(Blocks.HONEY_BLOCK);
    }
}
