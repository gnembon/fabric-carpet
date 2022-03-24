package carpet.mixins;

import carpet.utils.RandomTools;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;

@Mixin(FireworkRocketEntity.class)
public class FireworkRocketEntity_extremeMixin
{
    @Redirect(method = "<init>(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)V", expect = 2, at = @At(
            value = "INVOKE",
            target = "Ljava/util/Random;nextGaussian()D"
    ))
    private double nextGauBian(Random random)
    {
        return RandomTools.nextGauBian(random);
    }
}
