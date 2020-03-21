package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(World.class)
public abstract class World_fillUpdatesMixin
{
    @ModifyConstant(method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z",
            constant = @Constant(intValue = 16))
    private int addFillUpdatesInt(int original) {
        if (CarpetSettings.impendingFillSkipUpdates)
            return -1;
        return original;
    }

    @Redirect(method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;updateNeighbors(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/Block;)V"
    ))
    private void updateNeighborsMaybe(World world, BlockPos blockPos, Block block)
    {
        if (!CarpetSettings.impendingFillSkipUpdates) world.updateNeighbors(blockPos, block);
    }

}
