package carpet.mixins;

import carpet.fakes.SpawnHelperInnerInterface;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.PotentialCalculator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(NaturalSpawner.SpawnState.class)
public class SpawnStateMixin implements SpawnHelperInnerInterface
{
    @Shadow @Final private int spawnableChunkCount;

    @Shadow @Final private PotentialCalculator spawnPotential;

    //@Shadow @Final private Object2IntOpenHashMap<SpawnGroup> groupToCount;

    /*@Inject(method = "isBelowCap", at = @At("HEAD"), cancellable = true)
    private void changeMobCaps(SpawnGroup entityCategory, CallbackInfoReturnable<Boolean> cir)
    {
        int newCap = (int) ((double)entityCategory.getCapacity()*(Math.pow(2.0,(SpawnReporter.mobcap_exponent/4))));
        int i = newCap * spawningChunkCount / SpawnReporter.MAGIC_NUMBER;
        cir.setReturnValue(groupToCount.getInt(entityCategory) < i);
    }*/



    @Override
    public PotentialCalculator getPotentialCalculator()
    {
        return spawnPotential;
    }

    @Override
    public int cmGetChunkCount() {
        return spawnableChunkCount;
    }


}
