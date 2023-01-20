package carpet.mixins;

import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static carpet.script.CarpetEventServer.Event.PLAYER_PLACES_BLOCK;
import static carpet.script.CarpetEventServer.Event.PLAYER_PLACING_BLOCK;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;

@Mixin(BlockItem.class)
public class BlockItem_scarpetEventMixin
{
    @Inject(method = "place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;setPlacedBy(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)V",
            shift = At.Shift.AFTER
    ))
    private void afterPlacement(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir)
    {
        if (context.getPlayer() instanceof ServerPlayer && PLAYER_PLACES_BLOCK.isNeeded())
            PLAYER_PLACES_BLOCK.onBlockPlaced((ServerPlayer) context.getPlayer(), context.getClickedPos(), context.getHand(), context.getItemInHand());
    }
    
    @Inject(method = "placeBlock", at = @At("HEAD"), cancellable = true)
    private void beforePlacement(BlockPlaceContext blockPlaceContext, BlockState blockState, CallbackInfoReturnable<Boolean> cir) {
        if (blockPlaceContext.getPlayer() instanceof ServerPlayer && PLAYER_PLACING_BLOCK.isNeeded()) {
            if (PLAYER_PLACING_BLOCK.onBlockPlaced((ServerPlayer) blockPlaceContext.getPlayer(), blockPlaceContext.getClickedPos(), blockPlaceContext.getHand(), blockPlaceContext.getItemInHand())) {
                cir.setReturnValue(false);
                cir.cancel();
            }
        }
    }
}
