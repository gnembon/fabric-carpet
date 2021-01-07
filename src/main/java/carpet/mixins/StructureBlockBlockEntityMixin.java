package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.block.entity.StructureBlockBlockEntity;
import net.minecraft.structure.Structure;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ServerWorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;

@Mixin(StructureBlockBlockEntity.class)
public abstract class StructureBlockBlockEntityMixin
{
    @Redirect(method = "place", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/structure/Structure;place(Lnet/minecraft/world/ServerWorldAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/structure/StructurePlacementData;Ljava/util/Random;I)Z"
    ))
    private boolean onStructurePlacen(Structure structure, ServerWorldAccess serverWorldAccess, BlockPos pos, BlockPos blockPos, StructurePlacementData placementData, Random random, int i)
    {
        if(!CarpetSettings.fillUpdates)
            CarpetSettings.impendingFillSkipUpdates = true;
        try
        {
            return structure.place(serverWorldAccess, pos, blockPos, placementData, random, i); // serverWorldAccess, pos, placementData, random);
        }
        finally
        {
            CarpetSettings.impendingFillSkipUpdates = false;
        }
    }

    @ModifyConstant(
        method = "fromTag",
        constant = @Constant(intValue = 48)
    )
    private int positiveLimit(int original) {
        return CarpetSettings.structureBlockLimit;
    }

    @ModifyConstant(
        method = "fromTag",
        constant = @Constant(intValue = -48)
    )
    private int negativeLimit(int original) {
        return -CarpetSettings.structureBlockLimit;
    }
}
