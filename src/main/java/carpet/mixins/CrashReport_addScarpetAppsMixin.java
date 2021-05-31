package carpet.mixins;

import java.util.function.Supplier;
import java.util.stream.Collectors;

import net.minecraft.class_6396;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import carpet.CarpetServer;

@Mixin(class_6396.class)
public abstract class CrashReport_addScarpetAppsMixin
{
    @Shadow public abstract void method_37123(String string, Supplier<String> supplier);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void fillSystemDetails(CallbackInfo info) {
        if (CarpetServer.scriptServer == null || CarpetServer.scriptServer.modules.isEmpty()) return;
        method_37123("Loaded Scarpet Apps", () -> {
            return CarpetServer.scriptServer.modules.keySet().stream().collect(Collectors.joining("\n\t\t", "\n\t\t", ""));
        });
    }
}
