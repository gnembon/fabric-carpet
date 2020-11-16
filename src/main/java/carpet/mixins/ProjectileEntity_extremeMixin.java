package carpet.mixins;

import carpet.utils.RandomTools;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;

@Mixin(PersistentProjectileEntity.class)
public class ProjectileEntity_extremeMixin
{
    // calculates damage
    @Redirect(method = "applyEnchantmentEffects", expect = 1, at = @At(
            value = "INVOKE",
            target = "Ljava/util/Random;nextGaussian()D"
    ))
    private double nextGauBian2(Random random)
    {
        return RandomTools.nextGauBian(random);
    }

}
