package carpet.mixins;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static carpet.script.CarpetEventServer.Event.PISTON_EXTENDS;
import static carpet.script.CarpetEventServer.Event.PISTON_RETRACTS;

@Mixin(PistonBaseBlock.class)
public abstract class PistonBaseBlock_scarpetEventMixin extends DirectionalBlock
{
    @Shadow @Final private boolean isSticky;

    protected PistonBaseBlock_scarpetEventMixin(Properties settings)
    {
        super(settings);
    }

    @Inject(method = "moveBlocks", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(
            value = "INVOKE",
            target = "Lcom/google/common/collect/Maps;newHashMap()Ljava/util/HashMap;",
            shift = At.Shift.BEFORE
    ))
    private void onPistonExtendResolved(Level level, BlockPos pistonPos, Direction direction, boolean extending,
            CallbackInfoReturnable<Boolean> cir, BlockPos armPos, PistonStructureResolver resolver)
    {
        if (extending && PISTON_EXTENDS.isNeeded() && level instanceof ServerLevel serverLevel)
        {
            if (PISTON_EXTENDS.onPistonAction(serverLevel, pistonPos, direction, resolver.getToPush()))
            {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "triggerEvent", cancellable = true, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;defaultBlockState()Lnet/minecraft/world/level/block/state/BlockState;",
            ordinal = 0,
            shift = At.Shift.BEFORE
    ))
    private void onPistonRetractTriggered(BlockState state, Level level, BlockPos pos, int b0, int b1, CallbackInfoReturnable<Boolean> cir)
    {
        if (PISTON_RETRACTS.isNeeded() && level instanceof ServerLevel serverLevel)
        {
            Direction direction = state.getValue(FACING);
            List<BlockPos> toMove = willPullBlocks(level, pos, direction, b0) ? previewPulledBlocks(level, pos, direction) : List.of();
            if (PISTON_RETRACTS.onPistonAction(serverLevel, pos, direction, toMove))
            {
                cir.setReturnValue(false);
            }
        }
    }

    // mirrors the gate in triggerEvent's retract branch: moveBlocks is only invoked for a sticky piston
    // on a plain retract signal, with a pullable non-air block attached and no in-progress extension
    private boolean willPullBlocks(Level level, BlockPos pos, Direction direction, int b0)
    {
        if (!isSticky || b0 != 1)
        {
            return false;
        }
        BlockPos twoPos = pos.relative(direction, 2);
        BlockState movingState = level.getBlockState(twoPos);
        if (movingState.is(Blocks.MOVING_PISTON)
                && level.getBlockEntity(twoPos) instanceof PistonMovingBlockEntity entity
                && entity.getDirection() == direction && entity.isExtending())
        {
            return false;
        }
        return !movingState.isAir()
                && PistonBaseBlock.isPushable(movingState, level, twoPos, direction.getOpposite(), false, direction)
                && (movingState.getPistonPushReaction() == PushReaction.NORMAL
                        || movingState.is(Blocks.PISTON) || movingState.is(Blocks.STICKY_PISTON));
    }

    private List<BlockPos> previewPulledBlocks(Level level, BlockPos pos, Direction direction)
    {
        // the piston head would obstruct the resolve; moveBlocks removes it before resolving,
        // so mirror that here (same silent flags) and restore it right after the preview
        BlockPos armPos = pos.relative(direction);
        BlockState armState = level.getBlockState(armPos);
        boolean headSwapped = armState.is(Blocks.PISTON_HEAD);
        if (headSwapped)
        {
            level.setBlock(armPos, Blocks.AIR.defaultBlockState(), 276);
        }
        PistonStructureResolver resolver = new PistonStructureResolver(level, pos, direction, false);
        List<BlockPos> toMove = resolver.resolve() ? List.copyOf(resolver.getToPush()) : List.of();
        if (headSwapped)
        {
            level.setBlock(armPos, armState, 276);
        }
        return toMove;
    }
}
