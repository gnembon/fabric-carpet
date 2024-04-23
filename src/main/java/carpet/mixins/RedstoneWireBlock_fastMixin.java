package carpet.mixins;

import carpet.CarpetSettings;
import carpet.fakes.RedstoneWireBlockInterface;
import carpet.helpers.RedstoneWireTurbo;
import com.google.common.collect.Sets;
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

import static net.minecraft.world.level.block.RedStoneWireBlock.POWER;

@Mixin(RedStoneWireBlock.class)
public abstract class RedstoneWireBlock_fastMixin implements RedstoneWireBlockInterface {

    @Shadow
    private void updatePowerStrength(Level world_1, BlockPos blockPos_1, BlockState blockState_1) { }

    @Shadow
    private int calculateTargetStrength(Level world, BlockPos pos) { return 0; }

    @Override
    @Accessor("shouldSignal")
    public abstract void setWiresGivePower(boolean wiresGivePower);

    // =

    private RedstoneWireTurbo wireTurbo = null;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onRedstoneWireBlockCTOR(BlockBehaviour.Properties settings, CallbackInfo ci) {
        //noinspection ConstantConditions
        wireTurbo = new RedstoneWireTurbo((RedStoneWireBlock) (Object) this);
    }

    // =

    public void fastUpdate(Level world, BlockPos pos, BlockState state, BlockPos source) {
        // [CM] fastRedstoneDust -- update based on carpet rule
        if (CarpetSettings.fastRedstoneDust) {
            wireTurbo.updateSurroundingRedstone(world, pos, state, source);
            return;
        }
        updatePowerStrength(world, pos, state);
    }

    /**
     * @author theosib, soykaf, gnembon
     */
    @Inject(method = "updatePowerStrength", at = @At("HEAD"), cancellable = true)
    private void updateLogicAlternative(Level world, BlockPos pos, BlockState state, CallbackInfo cir) {
        if (CarpetSettings.fastRedstoneDust) {
            updateLogicPublic(world, pos, state);
            cir.cancel();
        }
    }

    @Override
    public BlockState updateLogicPublic(Level world_1, BlockPos blockPos_1, BlockState blockState_1) {
        int i = this.calculateTargetStrength(world_1, blockPos_1);
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


    @Redirect(method = "onPlace", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/RedStoneWireBlock;updatePowerStrength(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"))
    private void redirectOnBlockAddedUpdate(RedStoneWireBlock self, Level world_1, BlockPos blockPos_1, BlockState blockState_1) {
        fastUpdate(world_1, blockPos_1, blockState_1, null);
    }

    @Redirect(method = "onRemove", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/RedStoneWireBlock;updatePowerStrength(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"))
    private void redirectOnStateReplacedUpdate(RedStoneWireBlock self, Level world_1, BlockPos blockPos_1, BlockState blockState_1) {
        fastUpdate(world_1, blockPos_1, blockState_1, null);
    }

    @Redirect(method = "neighborChanged", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/RedStoneWireBlock;updatePowerStrength(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"))
    private void redirectNeighborUpdateUpdate(
            RedStoneWireBlock self,
            Level world_1,
            BlockPos blockPos_1,
            BlockState blockState_1,
            BlockState blockState_2,
            Level world_2,
            BlockPos blockPos_2,
            Block block_1,
            BlockPos blockPos_3,
            boolean boolean_1) {
        fastUpdate(world_1, blockPos_1, blockState_1, blockPos_3);
    }
}
