package carpet.fakes;

import net.minecraft.world.level.biome.Biome;

public interface BiomeInterface
{
    default Biome.ClimateSettings carpet$getClimateSettings() { throw new UnsupportedOperationException(); }
}
