package carpet.fakes;

import net.minecraft.core.BlockPos;

public interface ServerPlayerGameModeInterface
{
    default BlockPos carpet$getCurrentBreakingBlock() { throw new UnsupportedOperationException(); }

    default int carpet$getCurrentBlockBreakingProgress() { throw new UnsupportedOperationException(); }

    default void carpet$setBlockBreakingProgress(int progress) { throw new UnsupportedOperationException(); }
}
