package carpet.mixins;

import java.util.stream.Collectors;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import carpet.CarpetServer;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;

@Mixin(CrashReport.class)
public abstract class CrashReport_addScarpetAppsMixin
{
    @Shadow
    public abstract CrashReportSection getSystemDetailsSection();
    
    @Inject(at = @At("RETURN"), method = "fillSystemDetails")
    private void fillSystemDetails(CallbackInfo info) {
        if (CarpetServer.scriptServer == null || CarpetServer.scriptServer.modules.isEmpty()) return;
        getSystemDetailsSection().add("Loaded Scarpet Apps", () -> {
            return CarpetServer.scriptServer.modules.keySet().stream().collect(Collectors.joining("\n\t\t", "\n\t\t", ""));
        });
    }
}
