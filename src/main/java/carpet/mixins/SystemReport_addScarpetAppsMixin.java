package carpet.mixins;

import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.SystemReport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import carpet.CarpetServer;

@Mixin(SystemReport.class)
public abstract class SystemReport_addScarpetAppsMixin
{
    @Shadow public abstract void setDetail(String name, Supplier<String> valueSupplier);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void fillSystemDetails(CallbackInfo info) {
        if (CarpetServer.scriptServer == null || CarpetServer.scriptServer.modules.isEmpty()) return;
        setDetail("Loaded Scarpet Apps", () -> {
            return CarpetServer.scriptServer.modules.keySet().stream().collect(Collectors.joining("\n\t\t", "\n\t\t", ""));
        });
    }
}
