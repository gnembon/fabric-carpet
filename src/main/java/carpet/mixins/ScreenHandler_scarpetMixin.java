package carpet.mixins;


import carpet.script.value.ScreenHandlerValue;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerListener;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ScreenHandler.class)
public class ScreenHandler_scarpetMixin
{
    @Shadow @Final private List<ScreenHandlerListener> listeners;

    @Shadow @Final public List<Slot> slots;

    @Inject(method = "onSlotClick", at = @At(value = "HEAD"), cancellable = true)
    private void triggerClickListener(int i, int j, SlotActionType actionType, PlayerEntity playerEntity, CallbackInfoReturnable<ItemStack> cir)
    {
        this.listeners.forEach(screenHandlerListener ->
        {
            if(screenHandlerListener instanceof ScreenHandlerValue.ScarpetScreenHandlerListener)
            {
                if(((ScreenHandlerValue.ScarpetScreenHandlerListener) screenHandlerListener).onSlotClick(i,j,actionType,playerEntity))
                {
                    DefaultedList<ItemStack> defaultedList = DefaultedList.of();
                    for(int k = 0; k < this.slots.size(); ++k) {
                        defaultedList.add(this.slots.get(k).getStack());
                    }
                    if(playerEntity instanceof ServerPlayerEntity)
                        ((ServerPlayerEntity) playerEntity).onHandlerRegistered((ScreenHandler) (Object) this, defaultedList);
                    cir.setReturnValue(ItemStack.EMPTY);
                }
            }
        });
    }
}