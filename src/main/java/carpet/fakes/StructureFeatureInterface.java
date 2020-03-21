package carpet.fakes;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorConfig;

public interface StructureFeatureInterface
{
    boolean plopAnywhere(ServerWorld world, BlockPos pos);
    boolean gridAnywhere(ServerWorld world, BlockPos pos);
    boolean plopAnywhere(ServerWorld world, BlockPos pos, ChunkGenerator<? extends ChunkGeneratorConfig> generator, boolean wireOnly);
}
