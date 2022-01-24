package carpet.mixins;

import carpet.CarpetSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

@Mixin(StructureBlockEntity.class)
public abstract class StructureBlockBlockEntity_fillUpdatesMixin
{
    @Redirect(target = @Desc(args = {ServerLevel.class, boolean.class, StructureTemplate.class}, value = "loadStructure", ret = boolean.class, owner = StructureBlockEntity.class), at = @At(
            value = "INVOKE",
            desc = @Desc(owner = StructureTemplate.class, value = "placeInWorld", args = {ServerLevelAccessor.class, BlockPos.class, BlockPos.class, StructurePlaceSettings.class, Random.class, int.class}, ret = boolean.class)
    ))
    private boolean onStructurePlacen(StructureTemplate structure, ServerLevelAccessor serverWorldAccess, BlockPos pos, BlockPos blockPos, StructurePlaceSettings placementData, Random random, int i)
    {
        if(!CarpetSettings.fillUpdates)
            CarpetSettings.impendingFillSkipUpdates.set(true);
        try
        {
            return structure.placeInWorld(serverWorldAccess, pos, blockPos, placementData, random, i); // serverWorldAccess, pos, placementData, random);
        }
        finally
        {
            CarpetSettings.impendingFillSkipUpdates.set(false);
        }
    }
}