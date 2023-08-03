package carpet.mixins;

import carpet.fakes.LevelInterface;
import carpet.helpers.TickRateManager;
import carpet.fakes.MinecraftInterface;
import carpet.network.CarpetClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(Minecraft.class)
public class MinecraftMixin implements MinecraftInterface
{
    @Shadow public ClientLevel level;
    
    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("HEAD"))
    private void onCloseGame(Screen screen, CallbackInfo ci)
    {
        CarpetClient.disconnect();
    }
    
    @Inject(at = @At("HEAD"), method = "tick")
    private void onClientTick(CallbackInfo info) {
        if (this.level != null) {
            getTickRateManager().ifPresent(TickRateManager::tick);
            // hope server doesn't need to tick - should be handled by the server on its own
            if (!getTickRateManager().map(TickRateManager::runsNormally).orElse(true))
                CarpetClient.shapes.renewShapes();
        }
    }

    @Override
    public Optional<TickRateManager> getTickRateManager()
    {
        if (this.level != null) {
            return Optional.of(((LevelInterface)this.level).tickRateManager());
        }
        return Optional.empty();
    }
}
