package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CauldronBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(CauldronBlock.class)
public class CauldronBlock_stackableSBoxesMixin
{
    @Inject(method = "onUse", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;hasTag()Z",
            shift = At.Shift.BEFORE
    ))
    private void setSboxCount(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult arg5, CallbackInfoReturnable<Boolean> cir,
                              ItemStack itemStack, int i, Item item, Block block, ItemStack itemStack5)
    {
        if (CarpetSettings.stackableShulkerBoxes)
            itemStack5.setCount(itemStack.getCount());
    }
}
