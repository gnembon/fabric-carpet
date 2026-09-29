package carpet.mixins;

import carpet.CarpetSettings;
import carpet.helpers.RailUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.class)
public class BlockBehaviour_easyBUDMixin {
    @Inject(at = @At("HEAD"), method = "useItemOn")
    private void onUseItemOn(ItemStack itemStack,
                             BlockState blockState,
                             Level level,
                             BlockPos blockPos,
                             Player player,
                             InteractionHand interactionHand,
                             BlockHitResult blockHitResult,
                             CallbackInfoReturnable<InteractionResult> cir)
    {
        if (CarpetSettings.easyBUD) {
            if (blockState.getBlock() instanceof PoweredRailBlock &&
                player.isHolding(Items.REDSTONE) &&
                blockHitResult.getDirection().equals(Direction.UP) &&
                player.isCreative())
            {
                RailUtils.powerRailLine(blockPos, blockState.getValue(PoweredRailBlock.SHAPE), true, level);
            }
        }
    }
}
