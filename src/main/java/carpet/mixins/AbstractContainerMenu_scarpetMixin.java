package carpet.mixins;

import carpet.fakes.AbstractContainerMenuInterface;
import carpet.script.value.ScreenValue;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.DataSlot;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenu_scarpetMixin implements AbstractContainerMenuInterface
{
    @Shadow @Final private List<ContainerListener> containerListeners;
    @Shadow public abstract void sendAllDataToRemote();
    @Shadow @Final private List<DataSlot> dataSlots;

    @Inject(method = "doClick", at = @At("HEAD"), cancellable = true)
    private void callSlotClickListener(int slotIndex, int button, ClickType actionType, Player player, CallbackInfo ci) {
        if(!(player instanceof ServerPlayer serverPlayerEntity)) return;
        for(ContainerListener screenHandlerListener : this.containerListeners) {
            if(screenHandlerListener instanceof ScreenValue.ScarpetScreenHandlerListener scarpetScreenHandlerListener) {
                if(scarpetScreenHandlerListener.onSlotClick(serverPlayerEntity, actionType, slotIndex, button)) {
                    ci.cancel();
                    sendAllDataToRemote();
                }
            }
        }
    }

    @Inject(method = "removed", at = @At("HEAD"), cancellable = true)
    private void callCloseListener(Player player, CallbackInfo ci) {
        if(!(player instanceof ServerPlayer serverPlayerEntity)) return;
        for(ContainerListener screenHandlerListener : this.containerListeners) {
            if(screenHandlerListener instanceof ScreenValue.ScarpetScreenHandlerListener scarpetScreenHandlerListener) {
                scarpetScreenHandlerListener.onClose(serverPlayerEntity);
            }
        }
    }

    @Override
    public boolean callButtonClickListener(int button, Player player) {
        if(!(player instanceof ServerPlayer serverPlayerEntity)) return false;
        for(ContainerListener screenHandlerListener : containerListeners) {
            if(screenHandlerListener instanceof ScreenValue.ScarpetScreenHandlerListener scarpetScreenHandlerListener) {
                if(scarpetScreenHandlerListener.onButtonClick(serverPlayerEntity, button))
                    return true;
            }
        }
        return false;
    }

    @Override
    public boolean callSelectRecipeListener(ServerPlayer player, RecipeHolder<?> recipe, boolean craftAll) {
        for(ContainerListener screenHandlerListener : containerListeners) {
            if(screenHandlerListener instanceof ScreenValue.ScarpetScreenHandlerListener scarpetScreenHandlerListener) {
                if(scarpetScreenHandlerListener.onSelectRecipe(player, recipe, craftAll))
                    return true;
            }
        }
        return false;
    }

    @Override
    public DataSlot getDataSlot(int index) {
        return this.dataSlots.get(index);
    }
}