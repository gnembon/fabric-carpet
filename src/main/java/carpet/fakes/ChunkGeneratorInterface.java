package carpet.fakes;

import net.minecraft.world.biome.Biome;

public interface ChunkGeneratorInterface
{
    void initStrongholds();
    boolean canPlaceStrongholdInBiomeCM(Biome biome);
}
