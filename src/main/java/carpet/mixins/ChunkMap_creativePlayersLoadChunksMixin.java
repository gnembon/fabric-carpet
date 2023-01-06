package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkMap.class)
public class ChunkMap_creativePlayersLoadChunksMixin {

    @Inject(method = "skipPlayer(Lnet/minecraft/server/level/ServerPlayer;)Z", at = @At("HEAD"), cancellable = true)
    private void startProfilerSection(ServerPlayer serverPlayer, CallbackInfoReturnable<Boolean> cir)
    {
        if (!CarpetSettings.creativePlayersLoadChunks && serverPlayer.isCreative()) {
            cir.setReturnValue(true);
        }
    }
}
