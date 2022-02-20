package carpet.mixins;

import carpet.fakes.ChunkGeneratorInterface;
import net.minecraft.core.Holder;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(ChunkGenerator.class)
public abstract class ChunkGenerator_scarpetMixin implements ChunkGeneratorInterface
{
    @Shadow protected abstract void ensureGenerated();

    @Shadow protected abstract List<StructurePlacement> getPlacementsForFeature(Holder<ConfiguredStructureFeature<?, ?>> holder);

    @Override
    public void initStrongholds()
    {
        ensureGenerated();
    }

    @Override
    public List<StructurePlacement> getPlacementsForFeatureCM(ConfiguredStructureFeature<?, ?> structure) {
        return getPlacementsForFeature(Holder.direct(structure));
    }
}
