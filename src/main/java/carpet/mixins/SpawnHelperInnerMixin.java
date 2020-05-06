package carpet.mixins;

import carpet.fakes.SpawnHelperInnerInterface;
import carpet.utils.SpawnReporter;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.entity.EntityCategory;
import net.minecraft.util.math.GravityField;
import net.minecraft.world.SpawnHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// temporary mixin until naming for spawnhelper gets fixed.

@Mixin(SpawnHelper.Info.class)
public class SpawnHelperInnerMixin implements SpawnHelperInnerInterface
{
    //@Shadow @Final private int field_24394;

    //@Shadow @Final private Object2IntMap<EntityCategory> field_24395;

    //@Shadow @Final private class_5263 field_24396;

    @Shadow @Final private int spawningChunkCount;

    @Shadow @Final private Object2IntOpenHashMap<EntityCategory> categoryToCount;

    @Shadow @Final private GravityField densityField;


    @Inject(method = "isBelowCap", at = @At("HEAD"), cancellable = true)
    private void changeMobCaps(EntityCategory entityCategory, CallbackInfoReturnable<Boolean> cir)
    {
        int newCap = (int) ((double)entityCategory.getSpawnCap()*(Math.pow(2.0,(SpawnReporter.mobcap_exponent/4))));
        int i = newCap * spawningChunkCount / (int)Math.pow(17.0D, 2.0D);
        cir.setReturnValue(categoryToCount.getInt(entityCategory) < i);
    }

    @Override
    public GravityField getPotentialCalculator()
    {
        return densityField;
    }



}
