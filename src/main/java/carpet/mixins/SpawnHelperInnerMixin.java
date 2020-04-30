package carpet.mixins;

import carpet.utils.SpawnReporter;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.entity.EntityCategory;
import net.minecraft.world.SpawnHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// temporary mixin until naming for spawnhelper gets fixed.

@Mixin(SpawnHelper.class_5262.class)
public class SpawnHelperInnerMixin
{
    @Shadow @Final private int field_24394;

    @Shadow @Final private Object2IntMap<EntityCategory> field_24395;

    @Inject(method = "method_27826", at = @At("HEAD"), cancellable = true)
    private void changeMobCaps(EntityCategory entityCategory, CallbackInfoReturnable<Boolean> cir)
    {
        int newCap = (int) ((double)entityCategory.getSpawnCap()*(Math.pow(2.0,(SpawnReporter.mobcap_exponent/4))));
        int i = newCap * field_24394 / (int)Math.pow(17.0D, 2.0D);
        cir.setReturnValue(field_24395.getInt(entityCategory) < i);
    }

}
