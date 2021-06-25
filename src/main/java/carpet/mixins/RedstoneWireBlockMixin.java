package carpet.mixins;

import carpet.CarpetSettings;
import carpet.fakes.RedstoneWireBlockInterface;
import carpet.helpers.RedstoneWireTurbo;
import com.google.common.collect.Sets;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;
import java.util.Set;

import static net.minecraft.block.RedstoneWireBlock.POWER;

@Mixin(RedstoneWireBlock.class)
public abstract class RedstoneWireBlockMixin implements RedstoneWireBlockInterface {

    @Shadow
    private void update(World world_1, BlockPos blockPos_1, BlockState blockState_1) { }

    @Shadow
    private int getReceivedRedstonePower(World world, BlockPos pos) { return 0; }

    @Override
    @Accessor("wiresGivePower")
    public abstract void setWiresGivePower(boolean wiresGivePower);

    @Override
    @Accessor("wiresGivePower")
    public abstract boolean getWiresGivePower();

    // =

    private RedstoneWireTurbo wireTurbo = null;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onRedstoneWireBlockCTOR(Block.Settings settings, CallbackInfo ci) {
        //noinspection ConstantConditions
        wireTurbo = new RedstoneWireTurbo((RedstoneWireBlock) (Object) this);
    }

    // =

    public void fastUpdate(World world, BlockPos pos, BlockState state, BlockPos source) {
        // [CM] fastRedstoneDust -- update based on carpet rule
        if (CarpetSettings.fastRedstoneDust) {
            wireTurbo.updateSurroundingRedstone(world, pos, state, source);
            return;
        }
        update(world, pos, state);
    }

    /**
     * @author theosib, soykaf, gnembon
     */
    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void updateLogicAlternative(World world, BlockPos pos, BlockState state, CallbackInfo cir) {
        if (CarpetSettings.fastRedstoneDust) {
            updateLogicPublic(world, pos, state);
            cir.cancel();
        }
    }

    @Override
    public BlockState updateLogicPublic(World world_1, BlockPos blockPos_1, BlockState blockState_1) {
        int i = this.getReceivedRedstonePower(world_1, blockPos_1);
        BlockState blockState = blockState_1;
        if (blockState_1.get(POWER) != i) {
            blockState_1 = blockState_1.with(POWER, i);
            if (world_1.getBlockState(blockPos_1) == blockState) {
                world_1.setBlockState(blockPos_1, blockState_1, 2);
            }

            if (!CarpetSettings.fastRedstoneDust) {
                Set<BlockPos> set = Sets.newHashSet();
                set.add(blockPos_1);
                Direction[] var6 = Direction.values();
                int var7 = var6.length;

                for (int var8 = 0; var8 < var7; ++var8) {
                    Direction direction = var6[var8];
                    set.add(blockPos_1.offset(direction));
                }

                Iterator var10 = set.iterator();

                while (var10.hasNext()) {
                    BlockPos blockPos = (BlockPos) var10.next();
                    world_1.updateNeighborsAlways(blockPos, blockState_1.getBlock());
                }
            }
        }
        return blockState_1;
    }

    // =


    @Redirect(method = "onBlockAdded", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/RedstoneWireBlock;update(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V"))
    private void redirectOnBlockAddedUpdate(RedstoneWireBlock self, World world_1, BlockPos blockPos_1, BlockState blockState_1) {
        fastUpdate(world_1, blockPos_1, blockState_1, null);
    }

    @Redirect(method = "onStateReplaced", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/RedstoneWireBlock;update(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V"))
    private void redirectOnStateReplacedUpdate(RedstoneWireBlock self, World world_1, BlockPos blockPos_1, BlockState blockState_1) {
        fastUpdate(world_1, blockPos_1, blockState_1, null);
    }

    @Redirect(method = "neighborUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/RedstoneWireBlock;update(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V"))
    private void redirectNeighborUpdateUpdate(
            RedstoneWireBlock self,
            World world_1,
            BlockPos blockPos_1,
            BlockState blockState_1,
            BlockState blockState_2,
            World world_2,
            BlockPos blockPos_2,
            Block block_1,
            BlockPos blockPos_3,
            boolean boolean_1) {
        fastUpdate(world_1, blockPos_1, blockState_1, blockPos_3);
    }
}
