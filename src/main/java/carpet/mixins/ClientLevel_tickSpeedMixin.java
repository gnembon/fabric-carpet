package carpet.mixins;

import carpet.fakes.LevelInterface;
import carpet.helpers.TickRateManager;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevel_tickSpeedMixin implements LevelInterface
{

    private TickRateManager tickRateManager;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci)
    {
        this.tickRateManager = new TickRateManager();
    }

    @Override
    public TickRateManager tickRateManager()
    {
        return tickRateManager;
    }
}
