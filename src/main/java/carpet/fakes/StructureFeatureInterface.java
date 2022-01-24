package carpet.fakes;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public interface StructureFeatureInterface<C extends FeatureConfiguration>
{
    boolean plopAnywhere(ServerLevel world, BlockPos pos, ChunkGenerator generator, boolean wireOnly,Biome biome, C config);
}
