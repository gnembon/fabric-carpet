package carpet.mixins;

import carpet.fakes.AbstractContainerMenuInterface;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractCraftingMenu.class)
public class AbstractCraftingMenu_scarpetMixin
{
    @Inject(method = "handlePlacement",at = @At("HEAD"), cancellable = true)
    private void selectRecipeCallback(boolean craftAll, boolean b, RecipeHolder<?> recipe, ServerLevel serverLevel, Inventory player, CallbackInfoReturnable<RecipeBookMenu.PostPlaceAction> cir) {
        if(((AbstractContainerMenuInterface) this).callSelectRecipeListener((ServerPlayer) player.player ,recipe,craftAll))
            cir.setReturnValue(RecipeBookMenu.PostPlaceAction.NOTHING);
    }
}
