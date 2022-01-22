package carpet.fakes;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.recipe.Recipe;
import net.minecraft.screen.Property;
import net.minecraft.server.network.ServerPlayerEntity;

public interface ScreenHandlerInterface {
    Property getProperty(int index);
    boolean callButtonClickListener(int button, PlayerEntity player);
    boolean callSelectRecipeListener(ServerPlayerEntity player, Recipe<?> recipe, boolean craftAll);
}
