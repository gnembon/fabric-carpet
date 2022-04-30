package carpet.mixins;

import carpet.fakes.ChunkGeneratorInterface;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(ChunkGenerator.class)
public abstract class ChunkGenerator_scarpetMixin implements ChunkGeneratorInterface
{

    @Shadow public abstract void ensureStructuresGenerated(RandomState randomState);

    @Shadow protected abstract List<StructurePlacement> getPlacementsForStructure(Holder<Structure> holder, RandomState randomState);

    @Override
    public void initStrongholds(final ServerLevel level)
    {
        ensureStructuresGenerated(level.getChunkSource().randomState());
    }

    @Override
    public List<StructurePlacement> getPlacementsForFeatureCM(final ServerLevel level, Structure structure) {
        return getPlacementsForStructure(Holder.direct(structure), level.getChunkSource().randomState());
    }
}
