package carpet.mixins;

import carpet.utils.RandomTools;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;
import net.minecraft.world.entity.projectile.FishingHook;

@Mixin(FishingHook.class)
public class FishingHook_extremeMixin
{
    @Redirect(method = "<init>(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;II)V", expect =3, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/RandomSource;nextGaussian()D"
    ))
    private double nextGauBian(RandomSource random)
    {
        return RandomTools.nextGauBian(random);
    }
}
