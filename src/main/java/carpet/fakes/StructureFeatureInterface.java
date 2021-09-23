package carpet.fakes;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.random.ChunkRandom;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.FeatureConfig;

public interface StructureFeatureInterface<C>
{
    boolean plopAnywhere(ServerWorld world, BlockPos pos, ChunkGenerator generator, boolean wireOnly,Biome biome, FeatureConfig config);
    boolean shouldStartPublicAt(ChunkGenerator chunkGenerator, BiomeSource biomeSource, long l, ChunkRandom chunkRandom, ChunkPos chpos, ChunkPos chunkPos, C featureConfig, HeightLimitView heightLimitView);
}
