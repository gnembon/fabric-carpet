package carpet.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static carpet.script.CarpetEventServer.Event.BLOCK_DISPENSES;

@Mixin(DispenserBlock.class)
public abstract class DispenserBlock_scarpetEventMixin
{
    @Inject(method = "dispenseFrom", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/core/dispenser/DispenseItemBehavior;dispense(Lnet/minecraft/core/dispenser/BlockSource;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;",
            shift = At.Shift.BEFORE
    ))
    private void onDispense(ServerLevel level, BlockState state, BlockPos pos, CallbackInfo ci,
            DispenserBlockEntity blockEntity, BlockSource source, int slot, ItemStack itemstack, DispenseItemBehavior behavior)
    {
        if (BLOCK_DISPENSES.isNeeded() && BLOCK_DISPENSES.onBlockDispensed(level, pos, itemstack))
        {
            ci.cancel();
        }
    }
}
