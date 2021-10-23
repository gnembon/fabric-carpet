package carpet.mixins;

import carpet.fakes.ScreenHandlerInterface;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.screen.LecternScreenHandler;
import net.minecraft.screen.LoomScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.StonecutterScreenHandler;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


//classes that override onButtonClick
@Mixin({EnchantmentScreenHandler.class, LecternScreenHandler.class, LoomScreenHandler.class, StonecutterScreenHandler.class})
public abstract class ScreenHandlerSubclasses_scarpetMixin extends ScreenHandler {
    protected ScreenHandlerSubclasses_scarpetMixin(@Nullable ScreenHandlerType<?> type, int syncId) {
        super(type, syncId);
    }

    @Inject(method = "onButtonClick", at = @At("HEAD"), cancellable = true)
    private void buttonClickCallback(PlayerEntity player, int id, CallbackInfoReturnable<Boolean> cir) {
        if(((ScreenHandlerInterface) this).triggerButtonClickCallback(id,player))
            cir.cancel();
    }
}
