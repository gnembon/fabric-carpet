package carpet.mixins;

import carpet.CarpetServer;
import carpet.network.CarpetClient;
import carpet.settings.SettingsManager;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class LocalPlayer_clientCommandMixin
{
    @Inject(method = "chat", at = @At("HEAD"))
    private void inspectMessage(String string, CallbackInfo ci)
    {
        if (string.startsWith("/call "))
        {
            String command = string.substring(6);
            CarpetClient.sendClientCommand(command);
        }
        if (CarpetServer.minecraft_server == null && !CarpetClient.isCarpet())
        {
            LocalPlayer playerSource = (LocalPlayer)(Object) this;
            CarpetServer.settingsManager.inspectClientsideCommand(playerSource.createCommandSourceStack(), string);
            CarpetServer.extensions.forEach(e -> {
                SettingsManager sm = e.customSettingsManager();
                if (sm != null) sm.inspectClientsideCommand(playerSource.createCommandSourceStack(), string);
            });
        }
    }
}
