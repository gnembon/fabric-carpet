package carpet.mixins;

import carpet.utils.RandomTools;
import net.minecraft.entity.FireworkEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;

@Mixin(FireworkEntity.class)
public class FireworkEntity_extremeMixin
{
    @Redirect(method = "<init>(Lnet/minecraft/world/World;DDDLnet/minecraft/item/ItemStack;)V", expect = 2, at = @At(
            value = "INVOKE",
            target = "Ljava/util/Random;nextGaussian()D"
    ))
    private double nextGauBian(Random random)
    {
        return RandomTools.nextGauBian(random);
    }

    @Redirect(method = "setVelocity", expect = 3, at = @At(
            value = "INVOKE",
            target = "Ljava/util/Random;nextGaussian()D"
    ))
    private double nextGauBian2(Random random)
    {
        return RandomTools.nextGauBian(random);
    }

}
