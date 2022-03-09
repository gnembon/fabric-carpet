package carpet.fakes;

import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import java.util.List;

public interface ChunkGeneratorInterface
{
    void initStrongholds();
    List<StructurePlacement> getPlacementsForFeatureCM(final ConfiguredStructureFeature<?, ?> structure);
}
