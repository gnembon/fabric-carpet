package carpet.mixins;

import carpet.fakes.SpawnHelperInnerInterface;
import carpet.utils.SpawnReporter;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.PotentialCalculator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NaturalSpawner.SpawnState.class)
public class SpawnHelperInnerMixin implements SpawnHelperInnerInterface
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
