package carpet.mixins;

import carpet.CarpetSettings;
import carpet.fakes.LevelInterface;
import net.minecraft.world.level.redstone.NeighborUpdater;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.redstone.Orientation;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrierBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(BarrierBlock.class)
public class BarrierBlock_updateSuppressionBlockMixin extends Block {
    private boolean shouldPower = false;

    public BarrierBlock_updateSuppressionBlockMixin(Properties settings) { super(settings); }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return (shouldPower && direction == Direction.DOWN) ? 15 : 0;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, Orientation fromPos, boolean notify) {
        if (CarpetSettings.updateSuppressionBlock != -1) {
            if (true/*fromPos.equals(pos.above())*/) { // todo neighbour updates don't have source
                BlockState stateAbove = level.getBlockState(pos.above());
                if (stateAbove.is(Blocks.ACTIVATOR_RAIL) && !stateAbove.getValue(PoweredRailBlock.POWERED)) {
                    level.scheduleTick(pos, this, 1);
                    NeighborUpdater updater = ((LevelInterface)level).getNeighborUpdater();
                    if (updater instanceof CollectingNeighborUpdaterAccessor cnua)
                        cnua.setCount(cnua.getMaxChainedNeighborUpdates()-CarpetSettings.updateSuppressionBlock);
                }
            }
        }
        super.neighborChanged(state, level, pos, block, fromPos, notify);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos posAbove = pos.above();
        BlockState stateAbove = level.getBlockState(posAbove);
        if (stateAbove.is(Blocks.ACTIVATOR_RAIL) && !stateAbove.getValue(PoweredRailBlock.POWERED)) {
            shouldPower = true;
            level.setBlock(posAbove, stateAbove.setValue(PoweredRailBlock.POWERED, true), Block.UPDATE_CLIENTS | Block.UPDATE_NONE);
            shouldPower = false;
        }
    }
}
