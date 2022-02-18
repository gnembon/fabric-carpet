package carpet.mixins;

//import net.minecraft.world.level.levelgen.NoiseRouterData;
//import net.minecraft.world.level.levelgen.NoiseSampler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;


@Mixin(targets = "net.minecraft.world.level.levelgen.NoiseRouterData$QuantizedSpaghettiRarity")
public interface QuantizedSpaghettiRarityMixin_scarpetMixin {
    @Invoker
    static double invokeGetSpaghettiRarity3D(double d) {
        throw new AssertionError();
    }
}
