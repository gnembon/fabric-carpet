package carpet.mixins;

import carpet.fakes.ScreenHandlerSyncHandlerInterface;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "net.minecraft.server.network.ServerPlayerEntity$1")
public abstract class ScreenHandlerSyncHandler_scarpetMixin implements ScreenHandlerSyncHandlerInterface {
    @Override
    @Invoker
    public abstract void callSendPropertyUpdate(ScreenHandler handler, int property, int value);

    @Shadow(aliases = "field_29182")
    @Final
    public ServerPlayerEntity outer;

    @Override
    public ServerPlayerEntity getPlayer() {
        return outer;
    }
}
