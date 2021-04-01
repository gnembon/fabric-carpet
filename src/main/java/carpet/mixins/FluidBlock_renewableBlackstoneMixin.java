package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FluidBlock.class)
public abstract class FluidBlock_renewableBlackstoneMixin
{
    @Shadow @Final protected FlowableFluid fluid;

    @Shadow protected abstract void playExtinguishSound(WorldAccess world, BlockPos pos);

    @Inject(method = "receiveNeighborFluids", at = @At("TAIL"), cancellable = true)
    private void receiveFluidToBlackstone(World world, BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir)
    {
        if (CarpetSettings.renewableBlackstone)
        {
            if (fluid.isIn(FluidTags.LAVA)) {
                for(Direction direction : Direction.values())
                {
                    if (direction != Direction.DOWN) {
                        BlockPos blockPos = pos.method_35851(direction); // offset
                        if (world.getBlockState(blockPos).isOf(Blocks.BLUE_ICE)) {
                            world.setBlockState(pos, Blocks.BLACKSTONE.getDefaultState());
                            playExtinguishSound(world, pos);
                            cir.setReturnValue(false);
                        }
                    }
                }
            }
        }
    }
}
