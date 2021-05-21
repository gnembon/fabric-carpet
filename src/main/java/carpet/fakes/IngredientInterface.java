package carpet.fakes;

import net.minecraft.item.ItemStack;

import java.util.Collection;
import java.util.List;

public interface IngredientInterface
{
    /**
     * Gets all the stacks of the ingredients for a given item recipe. Also used for {@link carpet.helpers.HopperCounter#guessColor}
     * to guess the colour of an item to display it prettily
     */
    List<Collection<ItemStack>> getRecipeStacks();
}
