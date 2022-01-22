package carpet.fakes;

import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.FeatureConfig;

public interface PlacedFeatureInterface {
    ConfiguredFeature<?, ?> getRawFeature();
}
