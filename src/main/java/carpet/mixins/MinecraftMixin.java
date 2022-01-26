package carpet.mixins;

import carpet.CarpetServer;
import carpet.helpers.TickSpeed;
import carpet.network.CarpetClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin
{
    @Shadow public ClientLevel level;
    
    @Inject(method = "clearLevel(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("HEAD"))
    private void onCloseGame(Screen screen, CallbackInfo ci)
    {
        CarpetClient.disconnect();
    }
    
    @Inject(at = @At("HEAD"), method = "tick")
    private void onClientTick(CallbackInfo info) {
        if (this.level != null) {
            if (CarpetServer.minecraft_server == null)
                TickSpeed.tick();
            if (!TickSpeed.process_entities)
                CarpetClient.shapes.renewShapes();
        }
    }
}
