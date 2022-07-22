package carpet.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import carpet.fakes.BlockBehaviourInterface;
import carpet.fakes.BlockStateBaseInterface;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(BlockStateBase.class)
public class BlockStateBase_customStickyMixin implements BlockStateBaseInterface {

    @Shadow private Block getBlock() { return null; }
    @Shadow private BlockState asState() { return null; }

    @Override
    public boolean isSticky() {
        return ((BlockBehaviourInterface)getBlock()).isSticky(asState());
    }

    @Override
    public boolean isStickyToNeighbor(Level level, BlockPos pos, BlockPos neighborPos, BlockState neighborState, Direction dir, Direction moveDir) {
        return ((BlockBehaviourInterface)getBlock()).isStickyToNeighbor(level, pos, asState(), neighborPos, neighborState, dir, moveDir);
    }
}
