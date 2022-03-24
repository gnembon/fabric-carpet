package carpet.fakes;

import net.minecraft.core.BlockPos;

public interface ServerPlayerInteractionManagerInterface
{
    BlockPos getCurrentBreakingBlock();

    int getCurrentBlockBreakingProgress();

    void setBlockBreakingProgress(int progress);
}
