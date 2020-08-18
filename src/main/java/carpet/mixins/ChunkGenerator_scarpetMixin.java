package carpet.mixins;

import carpet.fakes.ChunkGeneratorInterface;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkGenerator.class)
public abstract class ChunkGenerator_scarpetMixin implements ChunkGeneratorInterface
{
    @Shadow protected abstract void generateStrongholdPositions();

    @Override
    public void initStrongholds()
    {
        generateStrongholdPositions();
    }
}
