package carpet.mixins;

import carpet.CarpetSettings;
import carpet.fakes.DefaultRedstoneWireEvaluatorInferface;
import carpet.fakes.RedstoneWireBlockInterface;
import carpet.helpers.RedstoneWireTurbo;
import com.google.common.collect.Sets;
import net.minecraft.world.level.redstone.DefaultRedstoneWireEvaluator;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.redstone.RedstoneWireEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

import static net.minecraft.world.level.block.RedStoneWireBlock.POWER;

@Mixin(RedStoneWireBlock.class)
public abstract class RedstoneWireBlock_fastMixin implements RedstoneWireBlockInterface {

    @Shadow
    private void updatePowerStrength(Level world_1, BlockPos blockPos_1, BlockState blockState_1, @Nullable final Orientation orientation, boolean sup) { }

    @Override
    @Accessor("shouldSignal")
    public abstract void setWiresGivePower(boolean wiresGivePower);

    @Override
    @Accessor("shouldSignal")
    public abstract boolean getWiresGivePower();

    private RedstoneWireEvaluator legacy = new DefaultRedstoneWireEvaluator((RedStoneWireBlock)(Object) this);

    // =

    private RedstoneWireTurbo wireTurbo = null;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onRedstoneWireBlockCTOR(BlockBehaviour.Properties settings, CallbackInfo ci) {
        //noinspection ConstantConditions
        wireTurbo = new RedstoneWireTurbo((RedStoneWireBlock) (Object) this);
    }

    // =

    public void fastUpdate(Level world, BlockPos pos, BlockState state, Orientation o, boolean sup) {
        // [CM] fastRedstoneDust -- update based on carpet rule
        if (CarpetSettings.fastRedstoneDust) {
            BlockPos source = null; // todo this probably removes all improvements from the original method
            // so needs to be evaluated if its worth keeping
            wireTurbo.updateSurroundingRedstone(world, pos, state, source);
            return;
        }
        updatePowerStrength(world, pos, state, o, sup);
    }

    /**
     * @author theosib, soykaf, gnembon
     */
    @Inject(method = "updatePowerStrength", at = @At("HEAD"), cancellable = true)
    private void updateLogicAlternative(Level world, BlockPos pos, BlockState state, Orientation orientation, boolean sup, CallbackInfo cir) {
        if (CarpetSettings.fastRedstoneDust) {
            updateLogicPublic(world, pos, state);
            cir.cancel();
        }
    }

    @Override
    public BlockState updateLogicPublic(Level world_1, BlockPos blockPos_1, BlockState blockState_1) {
        int i = ((DefaultRedstoneWireEvaluatorInferface)legacy).calculateTargetStrengthCM(world_1, blockPos_1);
        BlockState blockState = blockState_1;
        if (blockState_1.getValue(POWER) != i) {
            blockState_1 = blockState_1.setValue(POWER, i);
            if (world_1.getBlockState(blockPos_1) == blockState) {
                // [Space Walker] suppress shape updates and emit those manually to
                // bypass the new neighbor update stack.
                if (world_1.setBlock(blockPos_1, blockState_1, Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_CLIENTS))
                    wireTurbo.updateNeighborShapes(world_1, blockPos_1, blockState_1);
            }

            if (!CarpetSettings.fastRedstoneDust) {
                Set<BlockPos> set = Sets.newHashSet();
                set.add(blockPos_1);
                Direction[] var6 = Direction.values();
                int var7 = var6.length;

                for (int var8 = 0; var8 < var7; ++var8) {
                    Direction direction = var6[var8];
                    set.add(blockPos_1.relative(direction));
                }

                for (BlockPos blockPos : set) {
                    world_1.updateNeighborsAt(blockPos, blockState_1.getBlock());
                }
            }
        }
        return blockState_1;
    }

    // =


    @Redirect(method = "onPlace", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/RedStoneWireBlock;updatePowerStrength(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/redstone/Orientation;Z)V"))
    private void redirectOnBlockAddedUpdate(RedStoneWireBlock self, Level world_1, BlockPos blockPos_1, BlockState blockState_1, Orientation o, boolean sup) {
        fastUpdate(world_1, blockPos_1, blockState_1, o, sup);
    }

    @Redirect(method = "onRemove", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/RedStoneWireBlock;updatePowerStrength(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/redstone/Orientation;Z)V"))
    private void redirectOnStateReplacedUpdate(RedStoneWireBlock self, Level world_1, BlockPos blockPos_1, BlockState blockState_1, Orientation o, boolean sup) {
        fastUpdate(world_1, blockPos_1, blockState_1, o, sup);
    }

    @Redirect(method = "neighborChanged", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/block/RedStoneWireBlock;updatePowerStrength(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/redstone/Orientation;Z)V"
            //"Lnet/minecraft/world/level/block/RedStoneWireBlock;updatePowerStrength(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/redstone/Orientation;)V"
    ))
    //private void red(final RedStoneWireBlock instance, final Level level,
    //                 final BlockPos blockPos, final BlockState blockState, final Orientation orientation)
    private void redirectNeighborUpdateUpdate(
            RedStoneWireBlock self,
            Level world_1,
            BlockPos blockPos_1,
            BlockState blockState_1,
            Orientation o,
            boolean sup,
            BlockState blockState_2,
            Level world_2,
            BlockPos blockPos_2,
            Block block_1,
            Orientation o2,
            boolean b
            )
    {
        fastUpdate(world_1, blockPos_1, blockState_1, o, sup);
    }
}
