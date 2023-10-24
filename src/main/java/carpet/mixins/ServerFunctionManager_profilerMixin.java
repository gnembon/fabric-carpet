package carpet.mixins;

import carpet.utils.CarpetProfiler;
import net.minecraft.server.ServerFunctionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerFunctionManager.class)
public class ServerFunctionManager_profilerMixin
{
    CarpetProfiler.ProfilerToken currentSection;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void beforeDatapacks(CallbackInfo ci)
    {
    	currentSection = CarpetProfiler.start_section(null, "Datapacks", CarpetProfiler.TYPE.GENERAL);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void afterDatapacks(CallbackInfo ci)
    {
        CarpetProfiler.end_current_section(currentSection);
    }
}
