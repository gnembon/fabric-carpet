package carpet.fakes;

import net.minecraft.screen.ScreenHandler;

public interface ScreenHandlerSyncManagerInterface {
    void callSendPropertyUpdate(ScreenHandler handler, int property, int value);
}
