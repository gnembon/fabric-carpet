package carpet.fakes;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorConfig;

public interface StructureFeatureInterface
{
    public boolean plopAnywhere(IWorld world, BlockPos pos);
    public boolean plopAnywhere(IWorld world, BlockPos pos, ChunkGenerator<? extends ChunkGeneratorConfig> generator);
}
