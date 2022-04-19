package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Level.class)
public abstract class Level_fillUpdatesMixin
{
    @ModifyConstant(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", //setBlockState main
            constant = @Constant(intValue = 16))
    private int addFillUpdatesInt(int original) {
        if (CarpetSettings.impendingFillSkipUpdates.get())
            return -1;
        return original;
    }

    @Redirect(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At( //setBlockState main
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;blockUpdated(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;)V"
    ))
    private void updateNeighborsMaybe(Level world, BlockPos blockPos, Block block)
    {
        if (!CarpetSettings.impendingFillSkipUpdates.get()) world.blockUpdated(blockPos, block);
    }

}
