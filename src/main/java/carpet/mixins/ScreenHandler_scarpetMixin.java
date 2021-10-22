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

    @Inject(method = "internalOnSlotClick", at = @At(value = "HEAD"), cancellable = true)
    private void triggerClickListener(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci)
    {
        this.listeners.forEach(screenHandlerListener ->
        {
            if(screenHandlerListener instanceof ScreenHandlerValue.ScarpetScreenHandlerListener)
            {
                if(((ScreenHandlerValue.ScarpetScreenHandlerListener) screenHandlerListener).onSlotClick(slotIndex,button,actionType,player))
                {
                    ci.cancel();
                    syncState();
                }
            }
        });
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
}