package carpet.mixins;

import carpet.fakes.RecipeManagerInterface;
import com.google.common.collect.Lists;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
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
    public List<Recipe<?>> getAllMatching(RecipeType<?> type, ResourceLocation output, final RegistryAccess registryAccess)
    {
        Map<ResourceLocation, Recipe<?>> typeRecipes = recipes.get(type);
        // happens when mods add recipe to the registry without updating recipe manager
        if (typeRecipes == null) return Collections.emptyList();
        if (typeRecipes.containsKey(output)) return Collections.singletonList(typeRecipes.get(output));
        final Registry<Item> regs = registryAccess.registryOrThrow(Registries.ITEM);
        return Lists.newArrayList(typeRecipes.values().stream().filter(
                r -> regs.getKey(r.getResultItem(registryAccess).getItem()).equals(output)).collect(Collectors.toList()));
    }
}
