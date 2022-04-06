package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(StructureTemplate.class)
public class StructureTemplate_fillUpdatesMixin
{
    @Redirect( method = "placeInWorld", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/ServerLevelAccessor;blockUpdated(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;)V"
    ))
    private void skipUpdateNeighbours(ServerLevelAccessor serverWorldAccess, BlockPos pos, Block block)
    {
        if (!CarpetSettings.impendingFillSkipUpdates.get())
            serverWorldAccess.blockUpdated(pos, block);
    }

    @Redirect(method = "placeInWorld", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;getKnownShape()Z"
    ))
    private boolean skipPostprocess(StructurePlaceSettings structurePlacementData)
    {
        return structurePlacementData.getKnownShape() || CarpetSettings.impendingFillSkipUpdates.get();
    }
}
