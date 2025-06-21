package carpet.mixins;

import carpet.CarpetSettings;
import carpet.fakes.PistonBlockEntityInterface;
import com.google.common.collect.Lists;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import java.util.Map;

@Mixin(PistonBaseBlock.class)
public abstract class PistonBaseBlock_movableBEMixin extends DirectionalBlock
{
    protected PistonBaseBlock_movableBEMixin(Properties block$Settings_1)
    {
        super(block$Settings_1);
    }

    @Inject(method = "isPushable", cancellable = true, at = @At(value = "RETURN", ordinal = 3, shift = At.Shift.BEFORE))
    private static void movableCMD(BlockState blockState_1, Level world_1, BlockPos blockPos_1,
            Direction direction_1, boolean boolean_1, Direction direction_2, CallbackInfoReturnable<Boolean> cir)
    {
        Block block_1 = blockState_1.getBlock();
        //Make CommandBlocks movable, either use instanceof CommandBlock or the 3 cmd block objects,
        if (CarpetSettings.movableBlockEntities && block_1 instanceof CommandBlock)
        {
            cir.setReturnValue(true);
        }
    }
    
    private static boolean isPushableBlockEntity(Block block)
    {
        //Making PISTON_EXTENSION (BlockPistonMoving) pushable would not work as its createNewTileEntity()-method returns null
        return block != Blocks.ENDER_CHEST && block != Blocks.ENCHANTING_TABLE &&
                       block != Blocks.END_GATEWAY && block != Blocks.END_PORTAL && block != Blocks.MOVING_PISTON  &&
                       block != Blocks.SPAWNER
                       && block != Blocks.SCULK_SENSOR && block != Blocks.CALIBRATED_SCULK_SENSOR; // these have weird behaviour and crashes, #1473, also #1885
    }
    
    @Redirect(method = "isPushable", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;hasBlockEntity()Z"))
    private static boolean ifHasBlockEntity(BlockState blockState)
    {
        if (!blockState.hasBlockEntity())
        {
            return false;
        }
        else
        {
            return !(CarpetSettings.movableBlockEntities && isPushableBlockEntity(blockState.getBlock()));
        }
    }

    @Redirect(method = "isPushable", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;getPistonPushReaction()Lnet/minecraft/world/level/material/PushReaction;"
    ))
    private static PushReaction moveGrindstones(BlockState blockState)
    {
        if (CarpetSettings.movableBlockEntities && blockState.getBlock() == Blocks.GRINDSTONE) return PushReaction.NORMAL;
        return blockState.getPistonPushReaction();
    }

    @Inject(method = "moveBlocks", at = @At(value = "INVOKE", shift = At.Shift.BEFORE,
            target = "Ljava/util/List;size()I", ordinal = 3),locals = LocalCapture.CAPTURE_FAILHARD)
    private void onMove(Level world_1, BlockPos blockPos_1, Direction direction_1, boolean boolean_1,
                        CallbackInfoReturnable<Boolean> cir, BlockPos blockPos_2, PistonStructureResolver pistonHandler_1, Map<?, ?> map_1,
                        List<BlockPos> list_1, List<BlockState> list_2, List<?> list_3, BlockState[] blockStates_1,
                        Direction direction_2, int int_2, @Share("blockEntities") LocalRef<List<BlockEntity>> blockEntities)
    {
        //Get the blockEntities and remove them from the world before any magic starts to happen
        if (CarpetSettings.movableBlockEntities)
        {
            blockEntities.set(Lists.newArrayList());
            for (int i = 0; i < list_1.size(); ++i)
            {
                BlockPos blockpos = list_1.get(i);
                BlockEntity blockEntity = (list_2.get(i).hasBlockEntity()) ? world_1.getBlockEntity(blockpos) : null;
                blockEntities.get().add(blockEntity);
                if (blockEntity != null)
                {
                    //hopefully this call won't have any side effects in the future, such as dropping all the BlockEntity's items
                    //we want to place this same(!) BlockEntity object into the world later when the movement stops again
                    world_1.removeBlockEntity(blockpos);
                    blockEntity.setChanged();
                }
            }
        }
    }

    @WrapOperation(method = "moveBlocks", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlockEntity(Lnet/minecraft/world/level/block/entity/BlockEntity;)V",
            ordinal = 0))
    private void setCarriedOnMove(Level instance, BlockEntity blockEntity, Operation<Void> original,
            @Local(ordinal = 1) int index, @Share("blockEntities") LocalRef<List<BlockEntity>> blockEntities)
    {
        if (CarpetSettings.movableBlockEntities)
            ((PistonBlockEntityInterface) blockEntity).setCarriedBlockEntity(blockEntities.get().get(index));
        original.call(instance, blockEntity);
    }
}
