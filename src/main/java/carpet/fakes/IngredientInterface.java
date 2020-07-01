package carpet.fakes;

import net.minecraft.item.ItemStack;

import java.util.Collection;
import java.util.List;

public interface IngredientInterface
{
    List<Collection<ItemStack>> getRecipeStacks();
}
