package carpet.mixins;

import carpet.utils.RandomTools;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;
import net.minecraft.world.entity.projectile.Projectile;

@Mixin(Projectile.class)
public class Projectile_extremeMixin
{
    @Redirect(method = "shoot", expect = 3, at = @At( //TODO make sure it's correct after mojmap
            value = "INVOKE",
            target = "Lnet/minecraft/util/RandomSource;nextGaussian()D"
    ))
    private double nextGauBian(RandomSource random)
    {
        return RandomTools.nextGauBian(random);
    }
}
