package carpet.fakes;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandlerSyncHandler;

public interface ScreenHandlerInterface {
    void setAndUpdateProperty(int index, int value);
    ScreenHandlerSyncHandler getSyncHandler();
    boolean triggerButtonClickCallback(int button, PlayerEntity player);
}
