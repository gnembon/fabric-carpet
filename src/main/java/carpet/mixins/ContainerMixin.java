package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ScreenHandler.class)
public abstract class ContainerMixin
{

    @Shadow public abstract ItemStack onSlotClick(int int_1, int int_2, SlotActionType slotActionType_1, PlayerEntity playerEntity_1);
    
    @Shadow public abstract void sendContentUpdates();

    @Shadow @Final public DefaultedList<Slot> slots;

    @Inject( method = "method_30010", at = @At(value = "HEAD"), cancellable = true) // on slot click
    private void onThrowClick( int slotId, int clickData, SlotActionType actionType, PlayerEntity playerEntity, CallbackInfoReturnable<ItemStack> cir)
    {
        if (actionType == SlotActionType.THROW && CarpetSettings.ctrlQCraftingFix && playerEntity.getInventory().getCursorStack().isEmpty() && slotId >= 0)
        {
            ItemStack itemStack_1 = ItemStack.EMPTY;
            Slot slot_4 = slots.get(slotId);
            if (/*slot_4 != null && */slot_4.hasStack() && slot_4.canTakeItems(playerEntity))
            {
                if(slotId == 0 && clickData == 1)
                {
                    Item craftedItem = slot_4.getStack().getItem();
                    while(slot_4.hasStack() && slot_4.getStack().getItem() == craftedItem)
                    {
                        this.onSlotClick(slotId, 0, SlotActionType.THROW, playerEntity);
                    }
                    this.sendContentUpdates();
                    cir.setReturnValue(itemStack_1);
                    cir.cancel();
                }
            }
        }
    }
}
