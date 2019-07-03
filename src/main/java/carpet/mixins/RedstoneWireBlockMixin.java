package carpet.mixins;

import carpet.settings.CarpetSettings;
import carpet.fakes.RedstoneWireBlockInterface;
import carpet.helpers.RedstoneWireTurbo;
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

import java.util.Iterator;
import java.util.Set;

import static net.minecraft.block.RedstoneWireBlock.POWER;

@Mixin(RedstoneWireBlock.class)
public abstract class RedstoneWireBlockMixin implements RedstoneWireBlockInterface {
    // =

    @Shadow
    private boolean wiresGivePower;

    @Shadow
    @Final
    private Set<BlockPos> affectedNeighbors;

    @Shadow
    private int increasePower(int int_1, BlockState blockState_1) { return 0; };

    @Shadow
    private BlockState update(World world_1, BlockPos blockPos_1, BlockState blockState_1) { return null; };

    // =

    @Override
    public BlockState updateLogicPublic(World world_1, BlockPos blockPos_1, BlockState blockState_1) {
        return updateLogic(world_1, blockPos_1, blockState_1);
    }

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

    public BlockState fastUpdate(World world, BlockPos pos, BlockState state, BlockPos source) {
        // [CM] fastRedstoneDust -- update based on carpet rule
        if (CarpetSettings.fastRedstoneDust) {
            return wireTurbo.updateSurroundingRedstone(world, pos, state, source);
        }
        return update(world, pos, state);
    }

    /**
     * @author theosib, soykaf, gnembon
     */
    @Overwrite
    private BlockState updateLogic(World world_1, BlockPos blockPos_1, BlockState blockState_1) {
        BlockState blockState_2 = blockState_1;
        int int_1 = (Integer)blockState_1.get(POWER); // i
        this.wiresGivePower = false;
        int int_2 = world_1.getReceivedRedstonePower(blockPos_1); // k
        this.wiresGivePower = true;
        int int_3 = 0; // l
        if (!CarpetSettings.fastRedstoneDust || int_2 < 15) {
            Iterator var8 = Direction.Type.HORIZONTAL.iterator();

            label43:
            while(true) {
                while(true) {
                    if (!var8.hasNext()) {
                        break label43;
                    }

                    Direction direction_1 = (Direction)var8.next();
                    BlockPos blockPos_2 = blockPos_1.offset(direction_1);
                    BlockState blockState_3 = world_1.getBlockState(blockPos_2);
                    int_3 = this.increasePower(int_3, blockState_3);
                    BlockPos blockPos_3 = blockPos_1.up();
                    if (blockState_3.isSimpleFullBlock(world_1, blockPos_2) && !world_1.getBlockState(blockPos_3).isSimpleFullBlock(world_1, blockPos_3)) {
                        int_3 = this.increasePower(int_3, world_1.getBlockState(blockPos_2.up()));
                    } else if (!blockState_3.isSimpleFullBlock(world_1, blockPos_2)) {
                        int_3 = this.increasePower(int_3, world_1.getBlockState(blockPos_2.down()));
                    }
                }
            }
        }

        int int_4 = int_3 - 1;
        if (int_2 > int_4) int_4 = int_2;


        if (int_1 != int_4) {
            blockState_1 = (BlockState)blockState_1.with(POWER, int_4);
            if (world_1.getBlockState(blockPos_1) == blockState_2) {
                world_1.setBlockState(blockPos_1, blockState_1, 2);
            }

            if (!CarpetSettings.fastRedstoneDust) {
                this.affectedNeighbors.add(blockPos_1);
                Direction[] var14 = Direction.values();
                int var15 = var14.length;

                for (int var16 = 0; var16 < var15; ++var16) {
                    Direction direction_2 = var14[var16];
                    this.affectedNeighbors.add(blockPos_1.offset(direction_2));
                }
            }
        }

        return blockState_1;
    }

    // =


    @Redirect(method = "onBlockAdded", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/RedstoneWireBlock;update(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Lnet/minecraft/block/BlockState;"))
    private BlockState redirectOnBlockAddedUpdate(RedstoneWireBlock self, World world_1, BlockPos blockPos_1, BlockState blockState_1) {
        return fastUpdate(world_1, blockPos_1, blockState_1, null);
    }

    @Redirect(method = "onBlockRemoved", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/RedstoneWireBlock;update(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Lnet/minecraft/block/BlockState;"))
    private BlockState redirectOnBlockRemovedUpdate(RedstoneWireBlock self, World world_1, BlockPos blockPos_1, BlockState blockState_1) {
        return fastUpdate(world_1, blockPos_1, blockState_1, null);
    }

    @Redirect(method = "neighborUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/RedstoneWireBlock;update(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Lnet/minecraft/block/BlockState;"))
    private BlockState redirectNeighborUpdateUpdate(
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
        return fastUpdate(world_1, blockPos_1, blockState_1, blockPos_3);
    }
}
