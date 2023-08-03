package carpet.fakes;

import net.minecraft.world.level.levelgen.DensityFunction;

public interface RandomStateInterface
{
    default DensityFunction.Visitor carpet$getVisitor() { throw new UnsupportedOperationException(); }
}
