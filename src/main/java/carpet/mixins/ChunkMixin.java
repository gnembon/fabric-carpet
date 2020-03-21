package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Chunk.class)
public abstract class ChunkMixin
{
    @Inject(method = "markBlockForPostProcessing(Lnet/minecraft/util/math/BlockPos;)V",
        at =@At("HEAD"), cancellable = true)
    private void squashWarnings(BlockPos blockPos_1, CallbackInfo ci)
    {
        if (CarpetSettings.skipGenerationChecks) ci.cancel();
    }
}
