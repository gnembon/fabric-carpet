package carpet.script.utils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class RecipeHelper
{
    public static List<Recipe<?>> getRecipesForOutput(RecipeManager recipeManager, RecipeType<?> type, ResourceLocation id, Level level)
    {
        List<Recipe<?>> results = new ArrayList<>();


        ContextMap context = SlotDisplayContext.fromLevel(level);
        recipeManager.getRecipes().forEach(r -> {
            if (r.value().getType() == type)
            {
                for (RecipeDisplay recipeDisplay : r.value().display())
                {
                    recipeDisplay.result().resolveForStacks(context).forEach(stack -> {
                        if (BuiltInRegistries.ITEM.wrapAsHolder(stack.getItem()).unwrapKey().map(ResourceKey::location).orElseThrow(IllegalStateException::new).equals(id))
                        {
                            results.add(r.value());
                        }
                    });
                }
            }
        });
        return results;
    }

    public static List<Recipe<?>> getRecipesForOutput(RecipeManager recipeManager, ResourceLocation id, Level level)
    {
        List<Recipe<?>> results = new ArrayList<>();

        ContextMap context = SlotDisplayContext.fromLevel(level);
        recipeManager.getRecipes().forEach(r -> {
            for (RecipeDisplay recipeDisplay : r.value().display())
            {
                recipeDisplay.result().resolveForStacks(context).forEach(stack -> {
                    if (BuiltInRegistries.ITEM.wrapAsHolder(stack.getItem()).unwrapKey().map(ResourceKey::location).orElseThrow(IllegalStateException::new).equals(id))
                    {
                        results.add(r.value());
                    }
                });
            }

        });
        return results;
    }
}
