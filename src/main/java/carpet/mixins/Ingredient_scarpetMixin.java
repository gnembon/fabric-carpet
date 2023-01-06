package carpet.mixins;

import carpet.fakes.IngredientInterface;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

@Mixin(Ingredient.class)
public class Ingredient_scarpetMixin implements IngredientInterface
{
    @Shadow @Final private Ingredient.Value[] values;

    @Override
    public List<Collection<ItemStack>> getRecipeStacks()
    {
        List<Collection<ItemStack>> res = Arrays.stream(values).map(Ingredient.Value::getItems).collect(Collectors.toList());
        return res;
    }
}
