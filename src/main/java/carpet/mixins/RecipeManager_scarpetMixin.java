package carpet.mixins;

import carpet.fakes.RecipeManagerInterface;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

@Mixin(RecipeManager.class)
public class RecipeManager_scarpetMixin implements RecipeManagerInterface
{

    @Shadow private Map<RecipeType<?>, Map<ResourceLocation, RecipeHolder<?>>> recipes;

    @Override
    public List<Recipe<?>> getAllMatching(RecipeType<?> type, ResourceLocation itemId, final RegistryAccess registryAccess)
    {
        Map<ResourceLocation, RecipeHolder<?>> typeRecipes = recipes.get(type);
        // happens when mods add recipe to the registry without updating recipe manager
        if (typeRecipes == null) return List.of();
        RecipeHolder<?> recipeFromMap = typeRecipes.get(itemId);
        if (recipeFromMap != null)
            return List.of(recipeFromMap.value());
        Registry<Item> regs = registryAccess.registryOrThrow(Registries.ITEM);
        Item item = regs.get(itemId);
        return typeRecipes.values()
                .stream()
                .<Recipe<?>>map(RecipeHolder::value)
                .filter(r -> r.getResultItem(registryAccess).getItem() == item)
                .toList();
    }
}
