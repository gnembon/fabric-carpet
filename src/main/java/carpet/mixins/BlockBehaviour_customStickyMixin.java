package carpet.mixins;

import org.spongepowered.asm.mixin.Mixin;

import carpet.fakes.BlockBehaviourInterface;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(BlockBehaviour.class)
public class BlockBehaviour_customStickyMixin implements BlockBehaviourInterface {

    @Override
    public boolean isSticky(BlockState state) {
        return false;
    }

    @Override
    public boolean isStickyToNeighbor(Level level, BlockPos pos, BlockState state, BlockPos neighborPos, BlockState neighborState, Direction dir, Direction moveDir) {
        return false;
    }
}
