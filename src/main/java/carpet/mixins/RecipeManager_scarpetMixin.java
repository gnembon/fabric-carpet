package carpet.mixins;

import carpet.fakes.RecipeManagerInterface;
import com.google.common.collect.Lists;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.RecipeType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mixin(RecipeManager.class)
public class RecipeManager_scarpetMixin implements RecipeManagerInterface
{

    @Shadow private Map<RecipeType<?>, Map<Identifier, Recipe<?>>> recipes;

    @Override
    public List<Recipe<?>> getAllMatching(RecipeType type, Identifier output)
    {
        Map<Identifier, Recipe<?>> typeRecipes = recipes.get(type);
        if (typeRecipes.containsKey(output)) return Collections.singletonList(typeRecipes.get(output));
        return Lists.newArrayList(typeRecipes.values().stream().filter(
                r -> Registry.ITEM.getId(r.getOutput().getItem()).equals(output)).collect(Collectors.toList()));
    }
}
