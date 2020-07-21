package carpet.mixins;

import carpet.fakes.ServerPlayerInteractionManagerInterface;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
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
public class ServerPlayerInteractionManager_scarpetEventsMixin implements ServerPlayerInteractionManagerInterface
{
    @Shadow public ServerPlayerEntity player;

    @Shadow private boolean mining;

    @Shadow private BlockPos miningPos;

    @Shadow private int blockBreakingProgress;

    @Shadow public ServerWorld world;

    @Inject(method = "tryBreakBlock", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/Block;onBreak(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/entity/player/PlayerEntity;)V",
            shift = At.Shift.BEFORE
    ))
    private void onBlockBroken(BlockPos blockPos_1, CallbackInfoReturnable<Boolean> cir, BlockState blockState_1, BlockEntity be, Block b)
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

    @Override
    public BlockPos getCurrentBreakingBlock()
    {
        if (!mining) return null;
        return miningPos;
    }

    @Override
    public int getCurrentBlockBreakingProgress()
    {
        if (!mining) return -1;
        return blockBreakingProgress;
    }

    @Override
    public void setBlockBreakingProgress(int progress)
    {
        blockBreakingProgress = MathHelper.clamp(progress, -1, 10);
        world.setBlockBreakingInfo(-1*this.player.getEntityId(), miningPos, blockBreakingProgress);
    }
}
