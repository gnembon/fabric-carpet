package carpet.mixins;

import carpet.utils.RandomTools;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;
import net.minecraft.world.entity.projectile.AbstractArrow;

@Mixin(AbstractArrow.class)
public class AbstractArrow_extremeMixin
{
    // calculates damage
    @Redirect(method = "setEnchantmentEffectsFromEntity", expect = 1, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/RandomSource;nextGaussian()D"
    ))
    private double nextGauBian2(RandomSource random)
    {
        return RandomTools.nextGauBian(random);
    }

}
