package carpet.mixins;

import carpet.utils.RandomTools;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;
import net.minecraft.world.Containers;

@Mixin(Containers.class)
public class Containers_extremeMixin
{
    @Redirect(method = "dropItemStack(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)V",  expect = 3, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/RandomSource;nextGaussian()D"
    ))
    private static double nextGauBian(RandomSource random)
    {
        return RandomTools.nextGauBian(random);
    }
}
