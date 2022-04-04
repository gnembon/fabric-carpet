package carpet.mixins;

import carpet.logging.logHelpers.UpdateStackCountHelper;
import net.minecraft.world.level.redstone.CollectingNeighborUpdater;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CollectingNeighborUpdater.class)
public class CollectingNeighborUpdater_loggerMixin {

    @Shadow
    private int count;


    @Inject(
            method = "runUpdates()V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/ArrayDeque;clear()V",
                    shift = At.Shift.BEFORE,
                    ordinal = 0
            )
    )
    private void onStackDone(CallbackInfo ci) {
        UpdateStackCountHelper.onStackCount(this.count);
    }
}
