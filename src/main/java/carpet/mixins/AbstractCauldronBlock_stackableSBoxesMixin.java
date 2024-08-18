package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractCauldronBlock.class)
public class AbstractCauldronBlock_stackableSBoxesMixin
{
    @Redirect(method = "useItemOn", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/core/cauldron/CauldronInteraction;interact(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/InteractionResult;"
    ))
    private InteractionResult wrapInteractor(final CauldronInteraction cauldronBehavior, final BlockState blockState, final Level world, final BlockPos blockPos, final Player playerEntity, final InteractionHand hand, final ItemStack itemStack)
    {
        int count = -1;
        if (CarpetSettings.shulkerBoxStackSize > 1 && itemStack.getItem() instanceof BlockItem && ((BlockItem)itemStack.getItem()).getBlock() instanceof ShulkerBoxBlock)
            count = itemStack.getCount();
        InteractionResult result = cauldronBehavior.interact(blockState, world, blockPos, playerEntity, hand, itemStack);
        if (count > 0 && result.consumesAction())
        {
            ItemStack current = playerEntity.getItemInHand(hand);
            if (current.getItem() instanceof BlockItem && ((BlockItem)itemStack.getItem()).getBlock() instanceof ShulkerBoxBlock)
                current.setCount(count);
        }
        return result;
    }
}
