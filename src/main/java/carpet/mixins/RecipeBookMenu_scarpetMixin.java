package carpet.mixins;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipeBookMenu.class)
public abstract class RecipeBookMenu_scarpetMixin extends AbstractContainerMenu
{
    private RecipeBookMenu_scarpetMixin(MenuType<?> type, int syncId)
    {
        super(type, syncId);
    }

    @Inject(method = "handlePlacement",at = @At("HEAD"), cancellable = true)
    private void selectRecipeCallback(boolean craftAll, Recipe<?> recipe, ServerPlayer player, CallbackInfo ci)
    {
        if(carpet$notifySelectRecipeListeners(player,recipe,craftAll))
            ci.cancel();
    }
}
