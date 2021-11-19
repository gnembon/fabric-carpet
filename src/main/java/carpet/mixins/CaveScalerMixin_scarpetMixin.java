package carpet.mixins;

import net.minecraft.world.gen.NoiseColumnSampler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;


@Mixin(NoiseColumnSampler.CaveScaler.class)
public interface CaveScalerMixin_scarpetMixin {
    @Invoker
    static double invokeScaleTunnels(double d) {
        throw new AssertionError();
    }
}
