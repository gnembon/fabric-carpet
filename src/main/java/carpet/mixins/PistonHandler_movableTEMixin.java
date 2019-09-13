package carpet.mixins;

import carpet.settings.CarpetSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
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
public abstract class PistonHandler_movableTEMixin
{
    /*
     * The following Mixins make double chests behave sticky on the side where they are connected to its other double chest half block.
     * This is achieved by Injecting calls to "stickToStickySide" where normally slimeblocks stick to all their neighboring blocks.
     * redirectGetBlockState_1_A/B is neccessary to get access to the blockState_1 variable, which is used in redirectSlimeBlock.
     * redirectSlimeBlock is neccessary to also enable chests to have the backward stickyness (this seems to be an edge case)
     *
     * Note that it is possible to separate chests the same way pistons can separate slimeblocks.
     */
    @Shadow
    @Final
    private World world;
    @Shadow protected abstract boolean tryMove(BlockPos blockPos_1, Direction direction_1);

    @Inject(method = "tryMove", at = @At(value = "INVOKE", target = "Ljava/util/List;get(I)Ljava/lang/Object;", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    /**
     * Handles blocks besides the slimeblock that are sticky. Currently only supports blocks that are sticky on one side.
     * @author 2No2Name
     */
    private void stickToStickySide(BlockPos blockPos_1, Direction direction_1, CallbackInfoReturnable<Boolean> cir, BlockState blockState_1, Block block_1, int int_1, int int_2, int int_4, BlockPos blockPos_3, int int_5, int int_6){
        if(!stickToStickySide(blockPos_3)){
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
    @Shadow @Final private List<BlockPos> movedBlocks;
    @Inject(method = "calculatePush", at = @At(value = "INVOKE", target = "Ljava/util/List;get(I)Ljava/lang/Object;", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    /**
     * Handles blocks besides the slimeblock that are sticky. Currently only supports blocks that are sticky on one side.
     * @author 2No2Name
     */
    private void stickToStickySide(CallbackInfoReturnable<Boolean> cir, int int_1){
        if(!stickToStickySide(this.movedBlocks.get(int_1))){
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    /**
     * Handles blocks besides the slimeblock that are sticky. Currently only supports blocks that are sticky on one side.
     * Currently the only additional sticky block is the double chest, which sticks to its other chest half.
     * @param blockPos_1 location of a block that moves and needs to stick other blocks to it
     * @author 2No2Name
     */
    private boolean stickToStickySide(BlockPos blockPos_1){
        if(!CarpetSettings.movableBlockEntities)
            return true;

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



    //Get access to the blockstate to check if it is a chest
    @Shadow private Direction direction;
    private BlockState blockState_1;
    @Redirect(method = "tryMove",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;", ordinal = 0))
    private BlockState redirectGetBlockState_1_A(World world, BlockPos pos) {
        return blockState_1 = world.getBlockState(pos);
    }
    @Redirect(method = "tryMove",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;", ordinal = 1))
    private BlockState redirectGetBlockState_1_B(World world, BlockPos pos) {
        return blockState_1 = world.getBlockState(pos);
    }
    @Redirect(method = "tryMove",
            at = @At(value = "FIELD", target = "Lnet/minecraft/block/Blocks;SLIME_BLOCK:Lnet/minecraft/block/Block;"))
    //Thanks to Earthcomputer for showing how to redirect FIELD access like this
    /**
     * Makes backwards stickyness work with sticky non-slimeblocks as well.
     * @author 2No2Nameb
     */
    private Block redirectSlimeBlock() {
        if (CarpetSettings.movableBlockEntities && isStickyOnSide(blockState_1, this.direction.getOpposite()))
            return blockState_1.getBlock(); //this makes the comparison in the while condition "while(blockState_1.getBlock() == redirectSlimeBlock())" evaluate to true, so the block is treated as sticky
        else
            return Blocks.SLIME_BLOCK; //vanilla behavior
    }



    //if more helpers like this start existing, move this to Chest class
    /**
     * @param blockState blockState of one double chest half block
     * @return Direction towards the other block of the double chest, null if the blockState is not a double chest
     * @author 2No2Name
     */
    private Direction getDirectionToOtherChestHalf(BlockState blockState){
        ChestType chestType;
        try{
            chestType = blockState.get(ChestBlock.CHEST_TYPE);
        }catch(IllegalArgumentException e){return null;}
        if(chestType == ChestType.SINGLE)
            return null;
        return ChestBlock.getFacing(blockState);
    }

    /**
     * Returns true if there is a modification making this blockState sticky on the given face. Vanilla stickyness of SLIME_BLOCK is not affected.
     * @param blockState BlockState to determine the stickyness of
     * @param direction Direction in which the stickyness is to be found
     * @return boolean whether block is not SLIME_BLOCK and is sticky in the given direction
     * @author 2No2Name
     */
    private boolean isStickyOnSide(BlockState blockState, Direction direction) {
        Block block = blockState.getBlock();
        if(block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST)
            //Make chests be sticky on the side to
            return getDirectionToOtherChestHalf(blockState) == direction;

        //example how you could make sticky pistons have a sticky side:
        //if(block == Blocks.STICKY_PISTON)
        //    return blockState.get(FacingBlock.FACING) == direction.getOpposite();
        return false;
    }
}
