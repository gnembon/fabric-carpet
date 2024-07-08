package carpet.mixins;

import carpet.fakes.RecipeManagerInterface;
import com.google.common.collect.Multimap;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collection;
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
    @Shadow private Multimap<RecipeType<?>, RecipeHolder<?>> byType;

    @Shadow private Map<ResourceLocation, RecipeHolder<?>> byName;

    @Override
    public List<Recipe<?>> getAllMatching(RecipeType<?> type, ResourceLocation itemId, RegistryAccess registryAccess)
    {
        // quiq cheq
        RecipeHolder<?> recipe = byName.get(itemId);
        if (recipe != null && recipe.value().getType().equals(type))
        {
            return List.of(recipe.value());
        }
        if (!byType.containsKey(type))
        {
            // happens when mods add recipe to the registry without updating recipe manager
            return List.of();
        }
        Collection<RecipeHolder<?>> typeRecipes = byType.get(type);
        Registry<Item> regs = registryAccess.registryOrThrow(Registries.ITEM);
        Item item = regs.get(itemId);
        return typeRecipes.stream()
                .<Recipe<?>>map(RecipeHolder::value)
                .filter(r -> r.getResultItem(registryAccess).getItem() == item)
                .toList();
    }
}
