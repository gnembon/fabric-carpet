package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.block.dispenser.BlockPlacementDispenserBehavior;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockPlacementDispenserBehavior.class)
public class BlockPlacementDispenserBehaviourMixin
{
    @Redirect(method = "dispenseSilently", at = @At(value="INVOKE", target="Lnet/minecraft/item/ItemStack;decrement(I)V"))
    private void cancleDoubleDecrement(ItemStack stack, int amount)
    {
        if (!CarpetSettings.b_stackableShulkerBoxes)
        {
            stack.decrement(amount);
        }
    }
}
