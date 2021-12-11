package carpet.mixins;


import carpet.fakes.ScreenHandlerInterface;
import carpet.script.value.ScreenValue;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.recipe.Recipe;
import net.minecraft.screen.Property;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerListener;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ScreenHandler.class)
public abstract class ScreenHandler_scarpetMixin implements ScreenHandlerInterface
{
    @Shadow @Final private List<ScreenHandlerListener> listeners;
    @Shadow public abstract void syncState();
    @Shadow @Final private List<Property> properties;

    @Inject(method = "internalOnSlotClick", at = @At("HEAD"), cancellable = true)
    private void callSlotClickListener(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        if(!(player instanceof ServerPlayerEntity serverPlayerEntity)) return;
        for(ScreenHandlerListener screenHandlerListener : this.listeners) {
            if(screenHandlerListener instanceof ScreenValue.ScarpetScreenHandlerListener scarpetScreenHandlerListener) {
                if(scarpetScreenHandlerListener.onSlotClick(serverPlayerEntity, actionType, slotIndex, button)) {
                    ci.cancel();
                    syncState();
                }
            }
        }
    }

    @Inject(method = "close", at = @At("HEAD"), cancellable = true)
    private void callCloseListener(PlayerEntity player, CallbackInfo ci) {
        if(!(player instanceof ServerPlayerEntity serverPlayerEntity)) return;
        for(ScreenHandlerListener screenHandlerListener : this.listeners) {
            if(screenHandlerListener instanceof ScreenValue.ScarpetScreenHandlerListener scarpetScreenHandlerListener) {
                scarpetScreenHandlerListener.onClose(serverPlayerEntity);
            }
        }
    }

    @Override
    public boolean callButtonClickListener(int button, PlayerEntity player) {
        if(!(player instanceof ServerPlayerEntity serverPlayerEntity)) return false;
        for(ScreenHandlerListener screenHandlerListener : listeners) {
            if(screenHandlerListener instanceof ScreenValue.ScarpetScreenHandlerListener scarpetScreenHandlerListener) {
                if(scarpetScreenHandlerListener.onButtonClick(serverPlayerEntity, button))
                    return true;
            }
        }
        return false;
    }

    @Override
    public boolean callSelectRecipeListener(ServerPlayerEntity player, Recipe<?> recipe, boolean craftAll) {
        for(ScreenHandlerListener screenHandlerListener : listeners) {
            if(screenHandlerListener instanceof ScreenValue.ScarpetScreenHandlerListener scarpetScreenHandlerListener) {
                if(scarpetScreenHandlerListener.onSelectRecipe(player, recipe, craftAll))
                    return true;
            }
        }
        return false;
    }

    @Override
    public Property getProperty(int index) {
        return this.properties.get(index);
    }
}