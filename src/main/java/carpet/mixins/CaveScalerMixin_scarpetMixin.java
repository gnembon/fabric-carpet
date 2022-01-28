package carpet.mixins;

import net.minecraft.world.level.levelgen.NoiseSampler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;


@Mixin(NoiseSampler.QuantizedSpaghettiRarity.class)
public interface CaveScalerMixin_scarpetMixin {
    @Invoker
    static double invokeGetSpaghettiRarity3D(double d) {
        throw new AssertionError();
    }
}
