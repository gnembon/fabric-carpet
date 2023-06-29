package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerStatus.class)
public class ServerStatus_motdMixin
{
    @Inject(method = "description", at = @At("HEAD"), cancellable = true)
    private void getDescriptionAlternative(CallbackInfoReturnable<Component> cir)
    {
        if (!CarpetSettings.customMOTD.equals("_"))
        {
            cir.setReturnValue(Component.literal(CarpetSettings.customMOTD));
            cir.cancel();
        }
    }
}
