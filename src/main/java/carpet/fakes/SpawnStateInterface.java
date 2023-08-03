package carpet.fakes;

import net.minecraft.world.level.PotentialCalculator;

public interface SpawnStateInterface
{
    default PotentialCalculator carpet$getPotentialCalculator() { throw new UnsupportedOperationException(); }
}
