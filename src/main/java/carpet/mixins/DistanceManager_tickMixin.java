package carpet.mixins;

import carpet.helpers.TickSpeed;
import net.minecraft.server.level.DistanceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DistanceManager.class)
public class DistanceManager_tickMixin
{
    @Shadow private long ticketTickCounter;

    @Inject(method = "purgeStaleTickets", at = @At("HEAD"))
    private void pauseTicketSystem(CallbackInfo ci)
    {
        // pausing expiry of tickets
        // that will prevent also chunks from unloading, so require a deep frozen state
        if (!TickSpeed.process_entities && TickSpeed.deeplyFrozen()) ticketTickCounter--;
    }
}
