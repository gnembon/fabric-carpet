package carpet.mixins;

import carpet.fakes.ChunkGeneratorInterface;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkGenerator.class)
public abstract class ChunkGenerator_scarpetMixin implements ChunkGeneratorInterface
{
    @Shadow protected abstract void generateStrongholds();
    @Shadow private static boolean validStrongholdBiome(Biome biome) { return true;}

        @Override
    public void initStrongholds()
    {
        generateStrongholds();
    }

    @Override
    public boolean canPlaceStrongholdInBiomeCM(Biome biome) {
        return validStrongholdBiome(biome);
    }
}
