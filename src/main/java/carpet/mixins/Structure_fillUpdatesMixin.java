package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.block.Block;
import net.minecraft.class_5425;
import net.minecraft.structure.Structure;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Structure.class)
public class Structure_fillUpdatesMixin
{
    @Redirect( method = "place(Lnet/minecraft/class_5425;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/structure/StructurePlacementData;Ljava/util/Random;I)Z", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/class_5425;updateNeighbors(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/Block;)V"
    ))
    private void skipUpdateNeighbours(class_5425 class_5425, BlockPos pos, Block block)
    {
        if (!CarpetSettings.impendingFillSkipUpdates)
            class_5425.updateNeighbors(pos, block);
    }

    @Redirect(method = "place(Lnet/minecraft/class_5425;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/structure/StructurePlacementData;Ljava/util/Random;I)Z", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/structure/StructurePlacementData;shouldUpdateNeighbors()Z"
    ))
    private boolean skipPostprocess(StructurePlacementData structurePlacementData)
    {
        return structurePlacementData.shouldUpdateNeighbors() || CarpetSettings.impendingFillSkipUpdates;
    }
}
