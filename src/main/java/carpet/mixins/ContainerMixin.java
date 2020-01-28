package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.container.Container;
import net.minecraft.container.Slot;
import net.minecraft.container.SlotActionType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Container.class)
public abstract class ContainerMixin
{
    @Shadow @Final public List<Slot> slots;

    @Shadow public abstract ItemStack onSlotClick(int int_1, int int_2, SlotActionType slotActionType_1, PlayerEntity playerEntity_1);
    
    @Shadow public abstract void sendContentUpdates();

    @Inject( method = "onSlotClick", at = @At(value = "HEAD"), cancellable = true)
    private void onThrowClick( int slotId, int clickData, SlotActionType actionType, PlayerEntity playerEntity, CallbackInfoReturnable<ItemStack> cir)
    {
        if (actionType == SlotActionType.THROW && CarpetSettings.ctrlQCraftingFix && playerEntity.inventory.getCursorStack().isEmpty() && slotId >= 0)
        {
            ItemStack itemStack_1 = ItemStack.EMPTY;
            Slot slot_4 = slots.get(slotId);
            if (slot_4 != null && slot_4.hasStack() && slot_4.canTakeItems(playerEntity))
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
