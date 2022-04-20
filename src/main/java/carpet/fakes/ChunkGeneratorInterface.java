package carpet.fakes;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import java.util.List;

public interface ChunkGeneratorInterface
{
    void initStrongholds(final ServerLevel level);
    List<StructurePlacement> getPlacementsForFeatureCM(final ServerLevel level,  final Structure structure);
}
