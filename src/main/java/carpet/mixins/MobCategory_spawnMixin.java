package carpet.mixins;

import carpet.fakes.SpawnGroupInterface;
import carpet.utils.SpawnReporter;
import net.minecraft.world.entity.MobCategory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobCategory.class)
public class MobCategory_spawnMixin implements SpawnGroupInterface
{
    @Shadow @Final private int max;

    @Inject(method = "getMaxInstancesPerChunk", at = @At("HEAD"), cancellable = true)
    private void getModifiedCapacity(CallbackInfoReturnable<Integer> cir)
    {
        cir.setReturnValue ((int) ((double)max*(Math.pow(2.0,(SpawnReporter.mobcap_exponent/4)))));
    }

    @Override
    public int getInitialSpawnCap()
    {
        return max;
    }
}
