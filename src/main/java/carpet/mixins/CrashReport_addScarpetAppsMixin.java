package carpet.mixins;

import java.util.function.Supplier;
import java.util.stream.Collectors;

import net.minecraft.util.SystemDetails;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import carpet.CarpetServer;

@Mixin(SystemDetails.class)
public abstract class CrashReport_addScarpetAppsMixin
{
    @Shadow public abstract void addSection(String name, Supplier<String> valueSupplier);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void fillSystemDetails(CallbackInfo info) {
        if (CarpetServer.scriptServer == null || CarpetServer.scriptServer.modules.isEmpty()) return;
        addSection("Loaded Scarpet Apps", () -> {
            return CarpetServer.scriptServer.modules.keySet().stream().collect(Collectors.joining("\n\t\t", "\n\t\t", ""));
        });
    }
}
