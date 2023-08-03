package carpet.fakes;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.crafting.Recipe;

public interface AbstractContainerMenuInterface
{
    default DataSlot carpet$getDataSlot(int index) { throw new UnsupportedOperationException(); }

    default boolean carpet$notifyButtonClickListeners(int button, Player player) { throw new UnsupportedOperationException(); }

    default boolean carpet$notifySelectRecipeListeners(ServerPlayer player, Recipe<?> recipe, boolean craftAll) { throw new UnsupportedOperationException(); }
}
