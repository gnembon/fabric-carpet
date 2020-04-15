package carpet.mixins;

import carpet.utils.RandomTools;
import net.minecraft.entity.projectile.FishingBobberEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;

@Mixin(FishingBobberEntity.class)
public class FishingBobberEntity_extremeMixin
{
    @Redirect(method = "<init>(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/World;II)V", expect =3, at = @At(
            value = "INVOKE",
            target = "Ljava/util/Random;nextGaussian()D"
    ))
    private double nextGauBian(Random random)
    {
        return RandomTools.nextGauBian(random);
    }
}
