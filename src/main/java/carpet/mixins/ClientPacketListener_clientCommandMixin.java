package carpet.mixins;

import carpet.CarpetServer;
import carpet.network.CarpetClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListener_clientCommandMixin extends ClientCommonPacketListenerImpl
{

    protected ClientPacketListener_clientCommandMixin(final Minecraft minecraft, final Connection connection, final CommonListenerCookie commonListenerCookie)
    {
        super(minecraft, connection, commonListenerCookie);
    }

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
            CarpetServer.forEachManager(sm -> sm.inspectClientsideCommand("/" + string));
        }
    }
}
