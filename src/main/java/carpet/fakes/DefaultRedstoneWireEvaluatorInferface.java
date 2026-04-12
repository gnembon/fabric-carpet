package carpet.fakes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;


public interface DefaultRedstoneWireEvaluatorInferface
{
    int calculateTargetStrengthCM(final Level level, final BlockPos pos);
}
