package carpet.fakes;

import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;

public interface ScreenHandlerSyncHandlerInterface {
    void callSendPropertyUpdate(ScreenHandler handler, int property, int value);
    ServerPlayerEntity getPlayer();
}
