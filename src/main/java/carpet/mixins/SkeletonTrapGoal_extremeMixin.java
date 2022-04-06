package carpet.mixins;

import carpet.utils.RandomTools;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;
import net.minecraft.world.entity.animal.horse.SkeletonTrapGoal;

@Mixin(SkeletonTrapGoal.class)
public class SkeletonTrapGoal_extremeMixin
{
    @Redirect(method = "tick", expect = 2, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/RandomSource;nextGaussian()D"
    ))
    private double nextGauBian(RandomSource random)
    {
        return RandomTools.nextGauBian(random);
    }
}
