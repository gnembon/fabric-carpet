package carpet.mixins;

import carpet.utils.RandomTools;
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
            target = "Ljava/util/Random;nextGaussian()D"
    ))
    private double nextGauBian(Random random)
    {
        return RandomTools.nextGauBian(random);
    }
}
