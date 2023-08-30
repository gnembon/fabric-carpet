package carpet.mixins;

import carpet.fakes.AbstractContainerMenuInterface;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipeBookMenu.class)
public class RecipeBookMenu_scarpetMixin {
    @Inject(method = "handlePlacement",at = @At("HEAD"), cancellable = true)
    private void selectRecipeCallback(boolean craftAll, RecipeHolder<?> recipe, ServerPlayer player, CallbackInfo ci) {
        if(((AbstractContainerMenuInterface) this).callSelectRecipeListener(player,recipe,craftAll))
            ci.cancel();
    }
}
