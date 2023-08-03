package carpet.mixins;

import carpet.CarpetSettings;
import carpet.fakes.RedStoneWireBlockInterface;
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
public abstract class RedstoneWireBlock_fastMixin implements RedStoneWireBlockInterface {

    @Shadow
    private void updatePowerStrength(Level level, BlockPos pos, BlockState state) { }

    @Shadow
    private int calculateTargetStrength(Level world, BlockPos pos) { return 0; }

    @Override
    @Accessor("shouldSignal")
    public abstract void carpet$setShouldSignal(boolean shouldSignal);

    @Override
    @Accessor("shouldSignal")
    public abstract boolean carpet$getShouldSignal();

    // =

    private RedstoneWireTurbo wireTurbo = null;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onRedstoneWireBlockCTOR(BlockBehaviour.Properties settings, CallbackInfo ci) {
        //noinspection ConstantConditions
        wireTurbo = new RedstoneWireTurbo((RedStoneWireBlock) (Object) this);
    }

    // =

    public void fastUpdate(Level level, BlockPos pos, BlockState state, BlockPos source) {
        // [CM] fastRedstoneDust -- update based on carpet rule
        if (CarpetSettings.fastRedstoneDust) {
            wireTurbo.updateSurroundingRedstone(level, pos, state, source);
            return;
        }
        updatePowerStrength(level, pos, state);
    }

    /**
     * @author theosib, soykaf, gnembon
     */
    @Inject(method = "updatePowerStrength", at = @At("HEAD"), cancellable = true)
    private void updateLogicAlternative(Level level, BlockPos pos, BlockState state, CallbackInfo cir) {
        if (CarpetSettings.fastRedstoneDust) {
            carpet$updateLogicPublic(level, pos, state);
            cir.cancel();
        }
    }

    @Override
    public BlockState carpet$updateLogicPublic(Level level, BlockPos pos, BlockState state) {
        int i = this.calculateTargetStrength(level, pos);
        BlockState prevState = state;
        if (state.getValue(POWER) != i) {
            state = state.setValue(POWER, i);
            if (level.getBlockState(pos) == prevState) {
                // [Space Walker] suppress shape updates and emit those manually to
                // bypass the new neighbor update stack.
                if (level.setBlock(pos, state, Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_CLIENTS))
                    wireTurbo.updateNeighborShapes(level, pos, state);
            }

            if (!CarpetSettings.fastRedstoneDust) {
                Set<BlockPos> set = Sets.newHashSet();
                set.add(pos);
                Direction[] var6 = Direction.values();
                int var7 = var6.length;

                for (int var8 = 0; var8 < var7; ++var8) {
                    Direction direction = var6[var8];
                    set.add(pos.relative(direction));
                }

                for (BlockPos blockPos : set) {
                    level.updateNeighborsAt(blockPos, state.getBlock());
                }
            }
        }
        return state;
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
