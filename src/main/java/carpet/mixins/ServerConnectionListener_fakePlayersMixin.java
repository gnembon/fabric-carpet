package carpet.mixins;

import carpet.CarpetSettings;
import carpet.patches.FakePlayerManager;
import net.minecraft.server.network.ServerConnectionListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerConnectionListener.class)
public class ServerConnectionListener_fakePlayersMixin {
    @Inject(
            method = "tick",
            at = @At("RETURN")
    )
    private void tickFakePlayers(CallbackInfo ci) {
        if (!CarpetSettings.fakePlayerTicksInEU) {
            FakePlayerManager.tick();
        }
    }
}
