package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.block.AbstractCauldronBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.class_5620;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractCauldronBlock.class)
public class AbstractCauldronBlock_stackableSBoxesMixin
{
    @Redirect(method = "onUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_5620;interact(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/item/ItemStack;)Lnet/minecraft/util/ActionResult;"))
    private ActionResult wrapInteractor(class_5620 class_5620, BlockState blockState, World world, BlockPos blockPos, PlayerEntity playerEntity, Hand hand, ItemStack itemStack)
    {
        int count = -1;
        if (CarpetSettings.stackableShulkerBoxes && itemStack.getItem() instanceof BlockItem && ((BlockItem)itemStack.getItem()).getBlock() instanceof ShulkerBoxBlock)
            count = itemStack.getCount();
        ActionResult result = class_5620.interact(blockState, world, blockPos, playerEntity, hand, itemStack);
        if (count > 0 && result.isAccepted())
        {
            ItemStack current = playerEntity.getStackInHand(hand);
            if (current.getItem() instanceof BlockItem && ((BlockItem)itemStack.getItem()).getBlock() instanceof ShulkerBoxBlock)
                current.setCount(count);
        }
        return result;
    }
}
