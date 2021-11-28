package carpet.fakes;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.Property;
import net.minecraft.screen.ScreenHandlerSyncHandler;

public interface ScreenHandlerInterface {
    Property getProperty(int index);
    ScreenHandlerSyncHandler getSyncHandler();
    boolean callButtonClickListener(int button, PlayerEntity player);
}
