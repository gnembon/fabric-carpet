package carpet.mixins;

import carpet.fakes.ScreenHandlerSyncManagerInterface;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "net.minecraft.server.network.ServerPlayerEntity$1")
public abstract class ScreenHandlerSyncManager_scarpetMixin implements ScreenHandlerSyncManagerInterface {
    @Override
    @Invoker
    public abstract void callSendPropertyUpdate(ScreenHandler handler, int property, int value);
}
