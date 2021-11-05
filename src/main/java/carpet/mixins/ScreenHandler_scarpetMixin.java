package carpet.mixins;


import carpet.fakes.ScreenHandlerInterface;
import carpet.fakes.ScreenHandlerSyncHandlerInterface;
import carpet.script.value.ScreenHandlerValue;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerListener;
import net.minecraft.screen.ScreenHandlerSyncHandler;
import net.minecraft.screen.slot.SlotActionType;
import org.jetbrains.annotations.Nullable;
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

    @Shadow public abstract void setProperty(int id, int value);

    @Shadow protected abstract void notifyPropertyUpdate(int index, int value);

    @Shadow @Nullable private ScreenHandlerSyncHandler syncHandler;

    @Shadow public abstract void syncState();

    @Inject(method = "internalOnSlotClick", at = @At("HEAD"), cancellable = true)
    private void callSlotClickListener(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci)
    {
        for(ScreenHandlerListener screenHandlerListener : listeners)
        {
            if(screenHandlerListener instanceof ScreenHandlerValue.ScarpetScreenHandlerListener scarpetScreenHandlerListener)
            {
                if(scarpetScreenHandlerListener.onSlotClick(player, actionType, slotIndex, button))
                {
                    ci.cancel();
                    syncState();
                }
            }
        }
    }

    @Inject(method = "close", at = @At("HEAD"), cancellable = true)
    private void callCloseListener(PlayerEntity player, CallbackInfo ci) {
        for(ScreenHandlerListener screenHandlerListener : listeners)
        {
            if(screenHandlerListener instanceof ScreenHandlerValue.ScarpetScreenHandlerListener scarpetScreenHandlerListener)
            {
                scarpetScreenHandlerListener.onClose(player);
            }
        }
    }

    @Override
    public void setAndUpdateProperty(int index, int value) {
        this.setProperty(index,value);
        this.notifyPropertyUpdate(index,value);
        if(this.syncHandler == null) return;
        ((ScreenHandlerSyncHandlerInterface) this.syncHandler).callSendPropertyUpdate((ScreenHandler) (Object) this,index,value);
    }

    @Override
    public ScreenHandlerSyncHandler getSyncHandler() {
        return this.syncHandler;
    }

    @Override
    public boolean callButtonClickListener(int button, PlayerEntity player) {
        for(ScreenHandlerListener screenHandlerListener : listeners)
        {
            if(screenHandlerListener instanceof ScreenHandlerValue.ScarpetScreenHandlerListener scarpetScreenHandlerListener)
            {
                if(scarpetScreenHandlerListener.onButtonClick(player, button))
                    return true;
            }
        }
        return false;
    }
}