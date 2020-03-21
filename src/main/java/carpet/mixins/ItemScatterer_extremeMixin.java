package carpet.mixins;

import carpet.utils.RandomTools;
import net.minecraft.util.ItemScatterer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;

@Mixin(ItemScatterer.class)
public class ItemScatterer_extremeMixin
{
    @Redirect(method = "spawn(Lnet/minecraft/world/World;DDDLnet/minecraft/item/ItemStack;)V",  expect = 3, at = @At(
            value = "INVOKE",
            target = "Ljava/util/Random;nextGaussian()D"
    ))
    private static double nextGauBian(Random random)
    {
        return RandomTools.nextGauBian(random);
    }
}
