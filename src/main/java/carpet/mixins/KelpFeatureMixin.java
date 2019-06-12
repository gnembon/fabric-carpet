package carpet.mixins;

import carpet.settings.CarpetSettings;
import net.minecraft.world.gen.feature.KelpFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;

@Mixin(KelpFeature.class)
public class KelpFeatureMixin
{
    @Redirect(method = "method_13460", at = @At(value = "INVOKE", target = "Ljava/util/Random;nextInt(I)I"))
    private int getRandom(Random random, int bound)
    {
        if (bound==10)
            return random.nextInt(bound);
        int limit = CarpetSettings.kelpGenerationGrowthLimit;
        if (limit == 0)
            return random.nextInt(bound);
        return 25-limit+random.nextInt(Math.min(23,limit+1));
    }
}
