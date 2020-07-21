package carpet.fakes;

import net.minecraft.util.math.BlockPos;

public interface ServerPlayerInteractionManagerInterface
{
    BlockPos getCurrentBreakingBlock();

    int getCurrentBlockBreakingProgress();

    void setBlockBreakingProgress(int progress);
}
