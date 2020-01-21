package carpet.fakes;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorConfig;

public interface StructureFeatureInterface
{
    boolean plopAnywhere(IWorld world, BlockPos pos);
    boolean gridAnywhere(IWorld world, BlockPos pos);
    boolean plopAnywhere(IWorld world, BlockPos pos, ChunkGenerator<? extends ChunkGeneratorConfig> generator, boolean wireOnly);
}
