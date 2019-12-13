package carpet.mixins;

import carpet.utils.RandomTools;
import net.minecraft.entity.projectile.ExplosiveProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;

@Mixin(ExplosiveProjectileEntity.class)
public class ExplosiveProjectileEntity_extremeMixin
{
    @Redirect(method = "<init>(Lnet/minecraft/entity/EntityType;Lnet/minecraft/entity/LivingEntity;DDDLnet/minecraft/world/World;)V",
            expect = 3, at = @At(
                value = "INVOKE",
                target = "Ljava/util/Random;nextGaussian()D"
    ))
    private double nextGauBian(Random random)
    {
        return RandomTools.nextGauBian(random);
    }
}
