package carpet.mixins;

import carpet.settings.CarpetSettings;
import net.minecraft.server.ServerMetadata;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerMetadata.class)
public class ServerMetadata_motdMixin
{
    @Inject(method = "getDescription", at = @At("HEAD"), cancellable = true)
    private void getDescriptionAlternative(CallbackInfoReturnable<Text> cir)
    {
        if (!CarpetSettings.customMOTD.equals("_"))
        {
            cir.setReturnValue(new LiteralText(CarpetSettings.customMOTD));
            cir.cancel();
        }
    }
}
