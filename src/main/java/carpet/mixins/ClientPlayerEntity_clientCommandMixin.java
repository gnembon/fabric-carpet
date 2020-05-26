package carpet.mixins;

import carpet.CarpetServer;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntity_clientCommandMixin
{
    @Inject(method = "sendChatMessage", at = @At("HEAD"))
    private void inspectMessage(String string, CallbackInfo ci)
    {
        ClientPlayerEntity playerSource = (ClientPlayerEntity)(Object) this;
        if (CarpetServer.minecraft_server == null)
            CarpetServer.settingsManager.inspectClientsideCommand(playerSource.getCommandSource(), string);
    }
}
