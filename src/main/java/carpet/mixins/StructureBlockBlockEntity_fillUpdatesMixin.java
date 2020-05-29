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

import java.util.Random;

@Mixin(StructureBlockBlockEntity.class)
public abstract class StructureBlockBlockEntity_fillUpdatesMixin
{
    @Redirect(method = "place", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/structure/Structure;place(Lnet/minecraft/world/WorldAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/structure/StructurePlacementData;Ljava/util/Random;)V"
    ))
    private void onStructurePlacen(Structure structure, WorldAccess world, BlockPos pos, StructurePlacementData placementData, Random random)
    {
        if(!CarpetSettings.fillUpdates)
            CarpetSettings.impendingFillSkipUpdates = true;
        try
        {
            structure.place(world, pos, placementData, random);
        }
        finally
        {
            CarpetSettings.impendingFillSkipUpdates = false;
        }
    }
}
