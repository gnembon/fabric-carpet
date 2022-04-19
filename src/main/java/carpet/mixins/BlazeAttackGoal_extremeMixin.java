package carpet.mixins;

import carpet.utils.RandomTools;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;

@Mixin(targets = "net.minecraft.world.entity.monster.Blaze$BlazeAttackGoal")
public class BlazeAttackGoal_extremeMixin
{
    @Redirect(method = "tick()V", expect = 2, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/RandomSource;nextGaussian()D"
    ))
    private double nextGauBian(RandomSource random)
    {
        return RandomTools.nextGauBian(random);
    }
}
