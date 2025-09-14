package carpet.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import carpet.CarpetSettings;
import carpet.helpers.ShulkerHelper;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

@Mixin(AbstractContainerMenu.class)
public class AbstractContainerMenu_shulkerBoxesMixin {
	@WrapOperation(
		method = "getRedstoneSignalFromContainer(Lnet/minecraft/world/Container;)I",
		at = @At(value = "INVOKE", target = "net/minecraft/world/Container.getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I")
	)
	private static int boxAwareFullCalculation(Container c, ItemStack stack, Operation<Integer> original) {
		return CarpetSettings.shulkerBoxStackSize > 1 && !CarpetSettings.emptyShulkerBoxStackAlways && ShulkerHelper.isEmptyBox(stack)
				? 1
				: original.call(c, stack);
	}
}
