package carpet.mixins;

import carpet.fakes.PlacedFeatureInterface;
import net.minecraft.class_6796;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.Supplier;

@Mixin(class_6796.class)
public class PlacedFeature_scarpetMixin implements PlacedFeatureInterface {

    @Shadow @Final private Supplier<ConfiguredFeature<?, ?>> field_35732;

    @Override
    public ConfiguredFeature<?, ?> getRawFeature() {
        return field_35732.get();
    }
}
