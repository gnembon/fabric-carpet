package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.block.Block;
import net.minecraft.structure.Structure;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Structure.class)
public class Structure_fillUpdatesMixin
{
    @Redirect( method = "method_15172", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/IWorld;updateNeighbors(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/Block;)V"
    ))
    private void skipUpdateNeighbours(IWorld iWorld, BlockPos var1, Block var2)
    {
        if (!CarpetSettings.impendingFillSkipUpdates)
            iWorld.updateNeighbors(var1, var2);
    }
}
