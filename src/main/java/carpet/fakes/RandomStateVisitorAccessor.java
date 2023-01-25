package carpet.fakes;

import net.minecraft.world.level.levelgen.DensityFunction;

public interface RandomStateVisitorAccessor {
    DensityFunction.Visitor getVisitor();
}
