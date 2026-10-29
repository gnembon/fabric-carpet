package carpet.mixins;

import carpet.fakes.SpawnGroupInterface;
import carpet.utils.SpawnReporter;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.MobCategory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MobCategory.class)
public class MobCategory_spawnMixin implements SpawnGroupInterface
{
    @Shadow @Final private int max;

    @ModifyReturnValue(method = "getMaxInstancesPerChunk", at = @At("RETURN"))
    private int getModifiedCapacity(int original)
    {
        return ((int) ((double)original*(Math.pow(2.0,(SpawnReporter.mobcap_exponent/4)))));
    }

    @Override
    public int getInitialSpawnCap()
    {
        return max;
    }
}