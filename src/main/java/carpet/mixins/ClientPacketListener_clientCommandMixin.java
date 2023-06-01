package carpet.mixins;

import carpet.CarpetServer;
import carpet.network.CarpetClient;
import carpet.api.settings.SettingsManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListener_clientCommandMixin
{
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "sendCommand", at = @At("HEAD"))
    private void inspectMessage(String string, CallbackInfo ci)
    {
        if (string.startsWith("call "))
        {
            String command = string.substring(5);
            CarpetClient.sendClientCommand(command);
        }
        if (CarpetServer.minecraft_server == null && !CarpetClient.isCarpet() && minecraft.player != null)
        {
            LocalPlayer playerSource = minecraft.player;
            CarpetServer.settingsManager.inspectClientsideCommand(playerSource.createCommandSourceStack(),  "/"+string);
            CarpetServer.extensions.forEach(e -> {
                SettingsManager sm = e.extensionSettingsManager();
                if (sm != null) sm.inspectClientsideCommand(playerSource.createCommandSourceStack(), "/"+string);
            });
        }
    }
}
