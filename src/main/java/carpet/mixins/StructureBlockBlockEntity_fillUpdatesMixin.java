package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.block.entity.StructureBlockBlockEntity;
import net.minecraft.structure.Structure;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(StructureBlockBlockEntity.class)
public abstract class StructureBlockBlockEntity_fillUpdatesMixin
{
    @Redirect(method = "place", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/structure/Structure;place(Lnet/minecraft/world/WorldAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/structure/StructurePlacementData;)V"
    ))
    private void onStructurePlacen(Structure structure, WorldAccess WorldAccess_1, BlockPos blockPos_1, StructurePlacementData structurePlacementData_1)
    {
        if(!CarpetSettings.fillUpdates)
            CarpetSettings.impendingFillSkipUpdates = true;
        try
        {
            structure.place(WorldAccess_1, blockPos_1, structurePlacementData_1);
        }
        finally
        {
            CarpetSettings.impendingFillSkipUpdates = false;
        }
    }
}
