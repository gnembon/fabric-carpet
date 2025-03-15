package carpet.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import carpet.CarpetSettings;
import carpet.helpers.ShulkerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

@Mixin(HopperBlockEntity.class)
public class HopperBlockEntity_shulkerBoxMixin {
	@Inject(method = "canMergeItems", at = @At("RETURN"), cancellable = true)
	private static void dontMergeEmptyBoxes(ItemStack s1, ItemStack s2, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() && CarpetSettings.shulkerBoxStackSize > 1 && !CarpetSettings.emptyShulkerBoxStackAlways) {
			// return was going to be true, so they're already equal everything (and empty if boxes):
			// Check they're boxes and prevent merge if so
			if (ShulkerHelper.isBox(s1)) {
				cir.setReturnValue(false);
			}
		}
	}
	
	@WrapOperation(
		method = {"isFullContainer", "inventoryFull"},
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I")
	)
	private static int boxAwareFullCalculation(ItemStack stack, Operation<Integer> original) {
		return CarpetSettings.shulkerBoxStackSize > 1 && !CarpetSettings.emptyShulkerBoxStackAlways && ShulkerHelper.isEmptyBox(stack)
				? 1
				: original.call(stack);
	}

}
