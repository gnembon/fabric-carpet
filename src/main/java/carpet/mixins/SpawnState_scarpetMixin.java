package carpet.mixins;

import carpet.fakes.SpawnStateInterface;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.PotentialCalculator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(NaturalSpawner.SpawnState.class)
public class SpawnState_scarpetMixin implements SpawnStateInterface
{
    @Shadow @Final private PotentialCalculator spawnPotential;

    @Override
    public PotentialCalculator carpet$getPotentialCalculator()
    {
        return spawnPotential;
    }
}
