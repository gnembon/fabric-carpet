package carpet.fakes;

import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;

public interface RandomStateVisitorAccessor {
    DensityFunction.Visitor getVisitor();
}
