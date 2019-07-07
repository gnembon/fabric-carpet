package carpet.mixins;

import carpet.CarpetServer;
import com.google.gson.JsonElement;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.level.LevelGeneratorType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IntegratedServer.class)
public class IntegratedServerMixin
{
    @Inject(method = "loadWorld", at = @At("HEAD"))
    private void onSetupServerIntegrated(String string_1, String string_2, long long_1, LevelGeneratorType levelGeneratorType_1, JsonElement jsonElement_1, CallbackInfo ci) {
        CarpetServer.onServerLoaded((IntegratedServer) (Object) this);
    }
}
