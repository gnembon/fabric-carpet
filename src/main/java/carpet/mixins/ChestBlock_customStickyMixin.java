package carpet.mixins;

import org.spongepowered.asm.mixin.Mixin;

import carpet.CarpetSettings;
import carpet.fakes.BlockBehaviourInterface;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

import static net.minecraft.world.level.block.ChestBlock.getConnectedDirection;

@Mixin(ChestBlock.class)
public class ChestBlock_customStickyMixin implements BlockBehaviourInterface {

    @Override
    public boolean isSticky(BlockState state) {
        return CarpetSettings.movableBlockEntities;
    }

    @Override
    public boolean isStickyToNeighbor(Level level, BlockPos pos, BlockState state, BlockPos neighborPos, BlockState neighborState, Direction dir, Direction moveDir) {
        if (!neighborState.is((Block)(Object)this)) {
            return false;
        }

        ChestType type = state.getValue(ChestBlock.TYPE);
        ChestType neighborType = neighborState.getValue(ChestBlock.TYPE);

        if (type == ChestType.SINGLE || neighborType == ChestType.SINGLE) {
            return false;
        }
        return getConnectedDirection(state) == dir;
    }
}
