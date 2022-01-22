package carpet.mixins;

import carpet.fakes.ChunkGeneratorInterface;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkGenerator.class)
public abstract class ChunkGenerator_scarpetMixin implements ChunkGeneratorInterface
{
    @Shadow protected abstract void generateStrongholdPositions();
    @Shadow private static boolean canPlaceStrongholdInBiome(Biome biome) { return true;}

        @Override
    public void initStrongholds()
    {
        generateStrongholdPositions();
    }

    @Override
    public boolean canPlaceStrongholdInBiomeCM(Biome biome) {
        return canPlaceStrongholdInBiome(biome);
    }
}
