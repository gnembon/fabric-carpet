package carpet.fakes;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface RedstoneWireBlockInterface {
    BlockState updateLogicPublic(World world_1, BlockPos blockPos_1, BlockState blockState_1);
    void setWiresGivePower(boolean wiresGivePower);
    boolean getWiresGivePower();
}
