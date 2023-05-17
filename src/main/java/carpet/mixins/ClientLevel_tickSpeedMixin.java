package carpet.mixins;

import carpet.fakes.ClientLevelInterface;
import carpet.helpers.TickRateManager;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class ClientLevel_tickSpeedMixin implements ClientLevelInterface
{

    private TickRateManager tickRateManager;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci)
    {
        this.tickRateManager = new TickRateManager();
    }

    @Override
    public TickRateManager getTickRateManager()
    {
        return tickRateManager;
    }
}
