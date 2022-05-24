package carpet.mixins;

import carpet.fakes.PlacedFeatureInterface;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.Supplier;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

@Mixin(PlacedFeature.class)
public class PlacedFeature_scarpetMixin implements PlacedFeatureInterface {

}
