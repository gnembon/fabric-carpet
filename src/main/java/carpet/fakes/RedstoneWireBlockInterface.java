package carpet.fakes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface RedstoneWireBlockInterface {
    BlockState updateLogicPublic(Level world_1, BlockPos blockPos_1, BlockState blockState_1);
    void setWiresGivePower(boolean wiresGivePower);
    boolean getWiresGivePower();
}
