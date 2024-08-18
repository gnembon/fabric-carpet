package carpet.mixins;

import carpet.fakes.DefaultRedstoneWireEvaluatorInferface;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.redstone.DefaultRedstoneWireEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DefaultRedstoneWireEvaluator.class)
public abstract class DefaultRedstoneWireEvaluator_redstoneMixin implements DefaultRedstoneWireEvaluatorInferface
{
    @Shadow protected abstract int calculateTargetStrength(final Level level, final BlockPos blockPos);

    @Override
    public int calculateTargetStrengthCM(final Level level, final BlockPos pos)
    {
        return calculateTargetStrength(level, pos);
    }
}
