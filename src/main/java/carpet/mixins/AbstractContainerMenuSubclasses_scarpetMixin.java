package carpet.mixins;

import carpet.fakes.AbstractContainerMenuInterface;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.LecternMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.StonecutterMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


//classes that override onButtonClick
@Mixin({EnchantmentMenu.class, LecternMenu.class, LoomMenu.class, StonecutterMenu.class})
public abstract class AbstractContainerMenuSubclasses_scarpetMixin extends AbstractContainerMenu {
    protected AbstractContainerMenuSubclasses_scarpetMixin(MenuType<?> type, int syncId) {
        super(type, syncId);
    }

    @Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
    private void buttonClickCallback(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
        if(((AbstractContainerMenuInterface) this).callButtonClickListener(id,player))
            cir.cancel();
    }
}
