package carpet.mixins;


import carpet.CarpetSettings;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;
import net.minecraft.world.entity.boss.wither.WitherBoss;

@Mixin(WitherBoss.class)
public class WitherBoss_extremeMixin
{
    @Redirect(method = "performRangedAttack(ILnet/minecraft/world/entity/LivingEntity;)V", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/RandomSource;nextFloat()F")
    )
    private float nextFloatAmplfied(RandomSource random)
    {
        if (CarpetSettings.extremeBehaviours) return random.nextFloat()/100;
        return random.nextFloat();
    }

}
