package carpet.fakes;

import net.minecraft.item.Item;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.util.Identifier;

import java.util.List;

public interface RecipeManagerInterface
{
    /**
     * Gets all the recipes for a given item. Also used for {@link carpet.helpers.HopperCounter#guessColor} to guess the
     * colour of an item to display it prettily
     */
    List<Recipe<?>> getAllMatching(RecipeType type, Identifier output);
}
