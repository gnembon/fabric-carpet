package carpet.fakes;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.crafting.RecipeHolder;

public interface AbstractContainerMenuInterface
{
    DataSlot getDataSlot(int index);
    boolean callButtonClickListener(int button, Player player);
    boolean callSelectRecipeListener(ServerPlayer player, RecipeHolder<?> recipe, boolean craftAll);
}
