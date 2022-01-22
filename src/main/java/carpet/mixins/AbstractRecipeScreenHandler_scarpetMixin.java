package carpet.mixins;

import carpet.fakes.ScreenHandlerInterface;
import net.minecraft.recipe.Recipe;
import net.minecraft.screen.AbstractRecipeScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractRecipeScreenHandler.class)
public class AbstractRecipeScreenHandler_scarpetMixin {
    @Inject(method = "fillInputSlots",at = @At("HEAD"), cancellable = true)
    private void selectRecipeCallback(boolean craftAll, Recipe<?> recipe, ServerPlayerEntity player, CallbackInfo ci) {
        if(((ScreenHandlerInterface) this).callSelectRecipeListener(player,recipe,craftAll))
            ci.cancel();
    }
}
