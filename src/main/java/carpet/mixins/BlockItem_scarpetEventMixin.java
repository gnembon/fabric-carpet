package carpet.mixins;

import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static carpet.script.CarpetEventServer.Event.PLAYER_PLACES_BLOCK;

@Mixin(BlockItem.class)
public class BlockItem_scarpetEventMixin
{
    @Inject(method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/Block;onPlaced(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;)V",
            shift = At.Shift.AFTER
    ))
    private void afterPlacement(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> cir)
    {
        if (context.getPlayer() instanceof ServerPlayerEntity && PLAYER_PLACES_BLOCK.isNeeded())
            PLAYER_PLACES_BLOCK.onBlockPlaced((ServerPlayerEntity) context.getPlayer(), context.getBlockPos(), context.getHand(), context.getStack());
    }
}
