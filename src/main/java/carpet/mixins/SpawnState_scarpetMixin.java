package carpet.mixins;

import carpet.fakes.SpawnHelperInnerInterface;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.PotentialCalculator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(NaturalSpawner.SpawnState.class)
public class SpawnState_scarpetMixin implements SpawnHelperInnerInterface
{
    @Shadow @Final private PotentialCalculator spawnPotential;

    @Override
    public PotentialCalculator getPotentialCalculator()
    {
        return spawnPotential;
    }
}
