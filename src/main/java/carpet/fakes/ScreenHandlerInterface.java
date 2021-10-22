package carpet.fakes;

import net.minecraft.screen.ScreenHandlerSyncHandler;

public interface ScreenHandlerInterface {
    void setAndUpdateProperty(int index, int value);
    ScreenHandlerSyncHandler getSyncHandler();
}
