package carpet.mixins;

import carpet.fakes.ServerPlayerInteractionManagerInterface;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static carpet.script.CarpetEventServer.Event.PLAYER_BREAK_BLOCK;
import static carpet.script.CarpetEventServer.Event.PLAYER_INTERACTS_WITH_BLOCK;


@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameMode_scarpetEventsMixin implements ServerPlayerInteractionManagerInterface
{
    @Shadow public ServerPlayer player;

    @Shadow private boolean isDestroyingBlock;

    @Shadow private BlockPos destroyPos;

    @Shadow private int lastSentState;

    @Shadow public ServerLevel level;

    @Inject(method = "destroyBlock", locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;playerWillDestroy(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/player/Player;)V",
            shift = At.Shift.BEFORE
    ))
    private void onBlockBroken(BlockPos blockPos_1, CallbackInfoReturnable<Boolean> cir, BlockState blockState_1, BlockEntity be, Block b)
    {
        if(PLAYER_BREAK_BLOCK.onBlockBroken(player, blockPos_1, blockState_1)) {
            this.level.sendBlockUpdated(blockPos_1, blockState_1, blockState_1, 3);
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @Inject(method = "useItemOn", at = @At(
            value = "RETURN",
            ordinal = 2
    ))
    private void onBlockActivated(ServerPlayer serverPlayerEntity, Level world, ItemStack stack, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir)
    {
        PLAYER_INTERACTS_WITH_BLOCK.onBlockHit(player, hand, hitResult);
    }

    @Override
    public BlockPos getCurrentBreakingBlock()
    {
        if (!isDestroyingBlock) return null;
        return destroyPos;
    }

    @Override
    public int getCurrentBlockBreakingProgress()
    {
        if (!isDestroyingBlock) return -1;
        return lastSentState;
    }

    @Override
    public void setBlockBreakingProgress(int progress)
    {
        lastSentState = Mth.clamp(progress, -1, 10);
        level.destroyBlockProgress(-1*this.player.getId(), destroyPos, lastSentState);
    }
}
