package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkRegion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkRegion.class)
public class ChunkRegion_scarpetPlopMixin
{
    @Inject(method = "markBlockForPostProcessing", at = @At("HEAD"))
    private void markOrNot(BlockPos blockPos, CallbackInfo ci)
    {
        if (CarpetSettings.skipGenerationChecks) ci.cancel();
    }
}
