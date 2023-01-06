package carpet.mixins;

import carpet.fakes.RecipeManagerInterface;
import com.google.common.collect.Lists;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

@Mixin(RecipeManager.class)
public class RecipeManager_scarpetMixin implements RecipeManagerInterface
{

    @Shadow private Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipes;

    @Override
    public List<Recipe<?>> getAllMatching(RecipeType<?> type, ResourceLocation output)
    {
        Map<ResourceLocation, Recipe<?>> typeRecipes = recipes.get(type);
        // happens when mods add recipe to the registry without updating recipe manager
        if (typeRecipes == null) return Collections.emptyList();
        if (typeRecipes.containsKey(output)) return Collections.singletonList(typeRecipes.get(output));
        return Lists.newArrayList(typeRecipes.values().stream().filter(
                r -> BuiltInRegistries.ITEM.getKey(r.getResultItem().getItem()).equals(output)).collect(Collectors.toList()));
    }
}
