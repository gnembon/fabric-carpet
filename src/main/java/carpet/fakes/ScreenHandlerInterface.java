package carpet.fakes;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.Property;

public interface ScreenHandlerInterface {
    Property getProperty(int index);
    boolean callButtonClickListener(int button, PlayerEntity player);
}
