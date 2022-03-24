package carpet.mixins;

import carpet.CarpetSettings;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Random;
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
    public int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        return (shouldPower && direction == Direction.DOWN) ? 15 : 0;
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean notify) {
        if (CarpetSettings.updateSuppressionBlockSetting != -1) {
            if (fromPos.equals(pos.above())) {
                BlockState stateAbove = world.getBlockState(fromPos);
                if (stateAbove.is(Blocks.ACTIVATOR_RAIL) && !stateAbove.getValue(PoweredRailBlock.POWERED)) {
                    if (CarpetSettings.updateSuppressionBlockSetting > 0) {
                        world.scheduleTick(pos, this, CarpetSettings.updateSuppressionBlockSetting);
                    }
                    throw new StackOverflowError("updateSuppressionBlock");
                }
            }
        }
        super.neighborChanged(state, world, pos, block, fromPos, notify);
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, Random random) {
        BlockPos posAbove = pos.above();
        BlockState stateAbove = world.getBlockState(posAbove);
        if (stateAbove.is(Blocks.ACTIVATOR_RAIL) && !stateAbove.getValue(PoweredRailBlock.POWERED)) {
            shouldPower = true;
            world.setBlock(posAbove, stateAbove.setValue(PoweredRailBlock.POWERED, true), 3);
            shouldPower = false;
        }
    }
}
