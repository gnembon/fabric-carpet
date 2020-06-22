package carpet.mixins;

import carpet.CarpetServer;
import net.minecraft.util.crash.CrashReport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CrashReport.class)
public class CrashReport_coreMixin
{
    @Inject(method = "initCrashReport", at = @At("RETURN"))
    private static void gameStarted(CallbackInfo ci)
    {
        CarpetServer.onGameStarted();
    }
}
