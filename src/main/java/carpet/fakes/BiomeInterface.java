package carpet.fakes;

import net.minecraft.world.gen.feature.ConfiguredStructureFeature;
import net.minecraft.world.gen.feature.StructureFeature;

public interface BiomeInterface
{
    ConfiguredStructureFeature<?, ?> getConfiguredFeature(StructureFeature<?> arg);
}
