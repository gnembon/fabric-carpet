package carpet.mixins;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static carpet.script.CarpetEventServer.Event.PLAYER_BREAK_BLOCK;
import static carpet.script.CarpetEventServer.Event.PLAYER_INTERACTS_WITH_BLOCK;


@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManager_scarpetEventsMixin
{
    @Shadow public ServerPlayerEntity player;

    @Inject(method = "tryBreakBlock", locals = LocalCapture.CAPTURE_FAILEXCEPTION, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/Block;onBroken(Lnet/minecraft/world/IWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V",
            shift = At.Shift.BEFORE
    ))
    private void onBlockBroken(BlockPos blockPos_1, CallbackInfoReturnable<Boolean> cir, BlockState blockState_1, BlockEntity be, Block b, boolean boolean_1)
    {
        PLAYER_BREAK_BLOCK.onBlockBroken(player, blockPos_1, blockState_1);
    }

    @Inject(method = "interactBlock", at = @At(
            value = "RETURN",
            ordinal = 2
    ))
    private void onBlockActivated(PlayerEntity playerArg, World world, ItemStack stack, Hand hand, BlockHitResult blockHitResult, CallbackInfoReturnable<ActionResult> cir)
    {
        PLAYER_INTERACTS_WITH_BLOCK.onBlockHit(player, hand, blockHitResult);
    }
}
