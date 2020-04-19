package carpet.mixins.blaze;

import net.minecraft.entity.ai.goal.RevengeGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(RevengeGoal.class)
public class RevengeGoalMixin
{
    @ModifyConstant(method = "callSameTypeForRevenge", constant = @Constant(doubleValue = 10.0), expect = 1)
    private double pushLimit(double original)
    {
        return 32D;
    }
}
