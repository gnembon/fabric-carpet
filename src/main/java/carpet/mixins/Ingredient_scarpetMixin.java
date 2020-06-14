package carpet.mixins;

import carpet.fakes.IngredientInterface;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Mixin(Ingredient.class)
public class Ingredient_scarpetMixin implements IngredientInterface
{
    @Shadow @Final private Ingredient.Entry[] entries;

    @Override
    public List<Collection<ItemStack>> getRecipeStacks()
    {
        List<Collection<ItemStack>> res = Arrays.stream(entries).map(Ingredient.Entry::getStacks).collect(Collectors.toList());
        return res;
    }
}
