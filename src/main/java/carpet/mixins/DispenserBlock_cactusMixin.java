package carpet.mixins;

import carpet.helpers.BlockRotator;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.DispenserBehavior;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DispenserBlock.class)
public abstract class DispenserBlock_cactusMixin
{
    @Inject(method = "getBehaviorForItem", at = @At("HEAD"), cancellable = true)
    private void registerCarpetBehaviors(ItemStack stack, CallbackInfoReturnable<DispenserBehavior> cir)
    {
        Item item = stack.getItem();
        if (item == Items.CACTUS)
            cir.setReturnValue(new BlockRotator.CactusDispenserBehaviour());
    }
}
