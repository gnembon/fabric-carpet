package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.structure.StructurePiece;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(StructurePiece.class)
public class StructurePiece_scarpetPlopMixin
{
    @Redirect(method = "addBlock", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/Chunk;markBlockForPostProcessing(Lnet/minecraft/util/math/BlockPos;)V"
    ))
    private void markOrNot(Chunk chunk, BlockPos pos)
    {
        if (!CarpetSettings.skipGenerationChecks) chunk.markBlockForPostProcessing(pos);
    }
}
