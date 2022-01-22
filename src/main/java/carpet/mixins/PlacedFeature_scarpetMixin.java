package carpet.mixins;

import carpet.fakes.PlacedFeatureInterface;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.PlacedFeature;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.Supplier;

@Mixin(PlacedFeature.class)
public class PlacedFeature_scarpetMixin implements PlacedFeatureInterface {

    @Shadow @Final private Supplier<ConfiguredFeature<?, ?>> feature;

    @Override
    public ConfiguredFeature<?, ?> getRawFeature() {
        return feature.get();
    }
}
