package carpet.fakes;

import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.util.Identifier;

import java.util.List;

public interface RecipeManagerInterface
{
    List<Recipe<?>> getAllMatching(RecipeType type, Identifier output);
}
