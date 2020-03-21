package carpet.fakes;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public interface BiomeArrayInterface
{
    void setBiomeAtIndex(BlockPos pos, World world, Biome what);
}
