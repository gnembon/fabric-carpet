package carpet.mixins;

import carpet.utils.RandomTools;
import net.minecraft.entity.ai.goal.SkeletonHorseTrapTriggerGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;

@Mixin(SkeletonHorseTrapTriggerGoal.class)
public class SkeletonHorseTrapTriggerGoal_extremeMixin
{
    @Redirect(method = "tick", expect = 2, at = @At(
            value = "INVOKE",
            target = "Ljava/util/Random;nextGaussian()D"
    ))
    private double nextGauBian(Random random)
    {
        return RandomTools.nextGauBian(random);
    }
}
