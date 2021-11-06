package carpet.fakes;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandlerSyncHandler;

public interface ScreenHandlerInterface {
    void setAndUpdateProperty(int index, int value);
    int getProperty(int index);
    ScreenHandlerSyncHandler getSyncHandler();
    boolean callButtonClickListener(int button, PlayerEntity player);
}
