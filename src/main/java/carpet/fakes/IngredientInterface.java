package carpet.fakes;

import java.util.Collection;
import java.util.List;
import net.minecraft.world.item.ItemStack;

public interface IngredientInterface
{
    /**
     * Gets all the stacks of the ingredients for a given item recipe. Also used for {@link carpet.helpers.HopperCounter#guessColor}
     * to guess the colour of an item to display it prettily
     */
    List<Collection<ItemStack>> getRecipeStacks();
}
