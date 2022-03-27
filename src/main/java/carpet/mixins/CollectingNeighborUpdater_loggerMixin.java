package carpet.mixins;

import carpet.logging.LoggerRegistry;
import carpet.utils.Messenger;
import net.minecraft.network.chat.BaseComponent;
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
        if(LoggerRegistry.__updateStackCount && this.count > 25) {
            LoggerRegistry.getLogger("updateStackCount").log(() -> {
                return new BaseComponent[]{Messenger.c(
                        "w Stack finished with: "+this.count+" updates"
                )};
            });
        }
    }
}
