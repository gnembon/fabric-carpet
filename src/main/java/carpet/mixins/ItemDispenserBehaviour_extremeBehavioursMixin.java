package carpet.mixins;

import carpet.utils.RandomTools;
import net.minecraft.block.dispenser.ItemDispenserBehavior;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;

@Mixin(ItemDispenserBehavior.class)
public class ItemDispenserBehaviour_extremeBehavioursMixin
{
    @Redirect(method = "spawnItem", expect = 3, at = @At(
            value = "INVOKE",
            target = "Ljava/util/Random;nextGaussian()D"
    ))
    private static double nextGauBian(Random random)
    {
        return RandomTools.nextGauBian(random);
    }

}
