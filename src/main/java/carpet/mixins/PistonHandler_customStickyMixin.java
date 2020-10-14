package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChainBlock;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.EndRodBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.block.piston.PistonHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(PistonHandler.class)
public abstract class PistonHandler_customStickyMixin
{
    /*
     * The following Mixins make double chests behave sticky on the side where they are connected to its other double chest half block.
     * This is achieved by Injecting calls to "stickToStickySide" where normally slimeblocks stick to all their neighboring blocks.
     * redirectGetBlockState_1_A/B is neccessary to get access to the blockState_1 variable, which is used in redirectSlimeBlock.
     * redirectSlimeBlock is neccessary to also enable chests to have the backward stickyness (this seems to be an edge case)
     *
     * Note that it is possible to separate chests the same way pistons can separate slimeblocks.
     *
     * These also support other custom sticky block with non-standard rules, like chains.
     */
    @Shadow @Final private World world;
    @Shadow @Final private Direction motionDirection;
    @Shadow protected abstract boolean tryMove(BlockPos blockPos_1, Direction direction_1);
    @Shadow private static boolean isBlockSticky(Block block_1) { return false; }
    @Shadow private static boolean isAdjacentBlockStuck(Block block, Block block2) {return false;}
    @Shadow protected abstract boolean canMoveAdjacentBlock(BlockPos pos);

    // collects information about sticking block when backtracking.
    private BlockPos currentPos;
    private BlockState currentState;
    private BlockState previousState;
    @Redirect(method = "tryMove", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;", ordinal = 0))
    private BlockState redirectGetBlockState_1_A(World world, BlockPos pos) {
        return currentState = previousState = world.getBlockState(pos);
    }
    @Redirect(method = "tryMove", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;", ordinal = 1))
    private BlockState redirectGetBlockState_1_B(World world, BlockPos pos) {
        previousState = currentState;
        currentPos = pos;
        return currentState = world.getBlockState(pos);
    }

    /**
     * Makes backwards stickiness work with sticky non-slimeblocks as well (chests, chains).
     * @author 2No2Name, gnembon
     */
    @Redirect(method = "tryMove", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/piston/PistonHandler;isBlockSticky(Lnet/minecraft/block/Block;)Z",
            ordinal = 0 )
    )
    private boolean redirectIsStickyBlock(Block block_1)
    {
        // applies to both MBE chests as well as sticky chains
        return blockCanBePulled(currentState) || isBlockSticky(block_1);
    }

    /**
     * Returns true if there is a modification making this blockState sticky on the given face. Vanilla stickyness of SLIME_BLOCK is not affected.
     * @param blockState BlockState to determine the stickyness of
     * @return boolean whether block is not SLIME_BLOCK and is sticky in the given direction
     * @author 2No2Name
     */
    private boolean blockCanBePulled(BlockState blockState) {
        if (CarpetSettings.movableBlockEntities)
        {
            Block block = blockState.getBlock();
            if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST)
                //Make chests be sticky on the side to
                return getDirectionToOtherChestHalf(blockState) == motionDirection.getOpposite();
            //example how you could make sticky pistons have a sticky side:
            //if(block == Blocks.STICKY_PISTON)
            //    return blockState.get(FacingBlock.FACING) == motionDirection;
        }
        if (CarpetSettings.chainStone && blockState.getBlock() == Blocks.CHAIN)
        {
            return isChainOnAxis(currentState, motionDirection);
        }


        return false;
    }

    /**
     * Determines if we should continue to drag blocks in a line (going opposite to the direction of a push)
     * @param previous: block that is already dragged
     * @param next: canidate block to decide if needs to be dragged along as well.
     * @return true if we should keep dragging blocks behind
     */
    @Redirect(method = "tryMove", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/piston/PistonHandler;isAdjacentBlockStuck(Lnet/minecraft/block/Block;Lnet/minecraft/block/Block;)Z")
    )
    private boolean isDraggingPreviousBlockBehind(Block previous, Block next)
    {
        if (CarpetSettings.chainStone)
        {
            if (previousState.getBlock() == Blocks.CHAIN && isChainOnAxis(previousState, motionDirection))
            {
                if ( (currentState.getBlock() == Blocks.CHAIN && isChainOnAxis(currentState, motionDirection))
                        || isEndRodOnAxis(currentState, motionDirection.getAxis())
                        || Block.sideCoversSmallSquare(world, currentPos, motionDirection.getOpposite()))
                {
                    return true;
                }
            }
        }
        return isAdjacentBlockStuck(previous, next);
    }


    /**
     * Handles blocks besides the slimeblock that are sticky. Currently only supports blocks that are sticky on one side.
     * This runs in U style structures with something in the middle on retraction.
     * @author 2No2Name
     */
    @Inject(method = "tryMove", locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true, at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;get(I)Ljava/lang/Object;",
            shift = At.Shift.AFTER
    ))
    private void stickToStickySide(BlockPos blockPos_1, Direction direction_1, CallbackInfoReturnable<Boolean> cir, BlockState blockState_1, Block block_1, int int_1, int int_2, int int_4, BlockPos blockPos_3, int int_5, int int_6)
    {
        if (CarpetSettings.movableBlockEntities)
        {
            if (!stickToStickySide(blockPos_3))
            {
                cir.setReturnValue(false);
            }
        }
    }
    @Shadow @Final private List<BlockPos> movedBlocks;
    @Inject(method = "calculatePush", at = @At(value = "INVOKE", target = "Ljava/util/List;get(I)Ljava/lang/Object;", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    /**
     * Handles blocks besides the slimeblock that are sticky.
     * Supports blocks that are sticky on one side, (double chests now, 2no2name)
     * as well as other special sticky blocks (chains, gnembon).
     * @author 2No2Name, gnembon
     */
    private void stickToStickySide(CallbackInfoReturnable<Boolean> cir, int int_1){
        if (CarpetSettings.movableBlockEntities)
        {
            if (!stickToStickySide(this.movedBlocks.get(int_1)))
            {
                cir.setReturnValue(false);
            }
        }

        if (CarpetSettings.chainStone)
        {
            BlockPos pos = this.movedBlocks.get(int_1);
            BlockState chainState = world.getBlockState(pos);
            // chain is sideways
            if (chainState.getBlock() == Blocks.CHAIN && !isChainOnAxis(chainState, motionDirection)
                    && !this.canMoveAdjacentBlock(pos))
            {
                cir.setReturnValue(false);
            }
        }
    }

    /**
     * Handles blocks besides the slimeblock that are sticky. Currently only supports blocks that are sticky on one side.
     * Currently the only additional sticky block is the double chest, which sticks to its other chest half.
     * @param blockPos_1 location of a block that moves and needs to stick other blocks to it
     * @author 2No2Name
     */
    private boolean stickToStickySide(BlockPos blockPos_1)
    {
        BlockState blockState_1 = this.world.getBlockState(blockPos_1);
        Block block = blockState_1.getBlock();
        Direction stickyDirection  = null;
        if(block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST) {
            stickyDirection = getDirectionToOtherChestHalf(blockState_1);
        }

        //example how you could make sticky pistons have a sticky side:
        //else if(block == Blocks.STICKY_PISTON){
        //    stickyDirection = blockState_1.get(FacingBlock.FACING);
        //}

        return stickyDirection == null || this.tryMove(blockPos_1.offset(stickyDirection), stickyDirection);
    }


    /**
     * This never seems to run
     * @author gnembon
     */
    @Inject(method = "tryMove", locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true, at= @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/piston/PistonHandler;isBlockSticky(Lnet/minecraft/block/Block;)Z",
            ordinal = 1,
            shift = At.Shift.BEFORE
            )
    )
    private void redirectIsStickyBlock(BlockPos pos, Direction dir, CallbackInfoReturnable<Boolean> cir,
                                       BlockState blockState, Block block, int i, int j, int l, BlockPos blockPos2, int m, int n, BlockPos blockPos3)
    {
        if (CarpetSettings.chainStone)
        {
            BlockState chainState = world.getBlockState(blockPos3);
            if (chainState.getBlock() == Blocks.CHAIN && !isChainOnAxis(chainState, motionDirection) && !canMoveAdjacentBlock(blockPos3))
            {
                cir.setReturnValue(false);
            }
        }
    }


    /**
     * Custom movement of blocks stuck to the sides of blocks other than slimeblocks like chains
     */
    @Inject(method = "canMoveAdjacentBlock", locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/piston/PistonHandler;isAdjacentBlockStuck(Lnet/minecraft/block/Block;Lnet/minecraft/block/Block;)Z",
            shift = At.Shift.BEFORE
    ))
    private void otherSideStickyCases(BlockPos pos, CallbackInfoReturnable<Boolean> cir,
                                      BlockState blockState, Direction var3[], int var4, int var5, Direction direction, BlockPos blockPos, BlockState blockState2)
    {
        if (CarpetSettings.chainStone)
        {
            if (blockState.getBlock() == Blocks.CHAIN && isChainOnAxis(blockState, direction))
            {
                if ((blockState2.getBlock() == Blocks.CHAIN && (blockState.get(ChainBlock.AXIS) == blockState2.get(ChainBlock.AXIS)))
                        || isEndRodOnAxis(blockState2, blockState.get(ChainBlock.AXIS))
                        || Block.sideCoversSmallSquare(world, blockPos, direction.getOpposite()))
                {
                    if (!tryMove(blockPos, direction))
                    {
                        cir.setReturnValue(false);
                    }
                }
            }
        }
    }

    /**
     * chains may cause moving adjacent blocks, meaning isAdjacentBlockStuck check is irreliable.
     * since initial block may not be sticky.
     * in vanilla canMoveAdjacent is always called on block2 being sticky.
     * @param block
     * @param block2
     * @return
     */
    @Redirect(method = "canMoveAdjacentBlock", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/piston/PistonHandler;isAdjacentBlockStuck(Lnet/minecraft/block/Block;Lnet/minecraft/block/Block;)Z")
    )
    private boolean isStuckSlimeStone(Block block, Block block2)
    {
        return isBlockSticky(block2) && isAdjacentBlockStuck(block, block2);
    }


    //if more helpers like this start existing, move this to Chest class
    /**
     * @param blockState blockState of one double chest half block
     * @return Direction towards the other block of the double chest, null if the blockState is not a double chest
     * @author 2No2Name
     */
    private Direction getDirectionToOtherChestHalf(BlockState blockState)
    {
        ChestType chestType;
        try{
            chestType = blockState.get(ChestBlock.CHEST_TYPE);
        }catch(IllegalArgumentException e){return null;}
        if(chestType == ChestType.SINGLE)
            return null;
        return ChestBlock.getFacing(blockState);
    }


    private boolean isChainOnAxis(BlockState state, Direction stickDirection)
    {
        Direction.Axis axis;
        try {
            axis = state.get(ChainBlock.AXIS);
        }catch(IllegalArgumentException e){return false;}
        return stickDirection.getAxis() == axis;
    }

    private boolean isEndRodOnAxis(BlockState state, Direction.Axis stickAxis)
    {
        if (state.getBlock() != Blocks.END_ROD) return false;
        Direction facing;
        try {
            facing = state.get(EndRodBlock.FACING);
        }catch(IllegalArgumentException e){return false;}
        return stickAxis == facing.getAxis();
    }
}
