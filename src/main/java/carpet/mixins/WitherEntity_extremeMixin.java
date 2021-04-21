package carpet.mixins;


import carpet.CarpetSettings;
import net.minecraft.entity.boss.WitherEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;

@Mixin(WitherEntity.class)
public class WitherEntity_extremeMixin
{
    @Redirect(method = "shootSkullAt(ILnet/minecraft/entity/LivingEntity;)V", at = @At(
            value = "INVOKE",
            target = "Ljava/util/Random;nextFloat()F")
    )
    private float nextFloatAmplfied(Random random)
    {
        if (CarpetSettings.extremeBehaviours) return random.nextFloat()/100;
        return random.nextFloat();
    }

}
