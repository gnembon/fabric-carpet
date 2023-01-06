package carpet.mixins;

import carpet.fakes.ChunkGeneratorInterface;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(ChunkGenerator.class)
public abstract class ChunkGenerator_scarpetMixin implements ChunkGeneratorInterface
{
    @Override
    public void initStrongholds(final ServerLevel level)
    {
        level.getChunkSource().getGeneratorState().ensureStructuresGenerated();
    }

    @Override
    public List<StructurePlacement> getPlacementsForFeatureCM(final ServerLevel level, Structure structure) {
        return level.getChunkSource().getGeneratorState().getPlacementsForStructure(Holder.direct(structure));
    }
}
