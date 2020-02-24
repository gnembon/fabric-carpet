package carpet.mixins;

import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

import static carpet.script.CarpetEventServer.Event.CHUNK_GENERATED;

@Mixin(ThreadedAnvilChunkStorage.class)
public class ThreadedAnvilChunkStorage_scarpetChunkCreationMixin
{
    @Shadow @Final private ServerWorld world;

    //in method_20617
    @Inject(method = "method_19534", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ThreadedAnvilChunkStorage;convertToFullChunk(Lnet/minecraft/server/world/ChunkHolder;)Ljava/util/concurrent/CompletableFuture;",
            shift = At.Shift.AFTER
    ))
    private void onChunkGenerated(ChunkHolder chunkHolder, Chunk chunk, CallbackInfoReturnable<CompletableFuture> cir)
    {
        if (CHUNK_GENERATED.isNeeded())
            world.getServer().execute( () -> CHUNK_GENERATED.onChunkGenerated(world, chunk) );
    }
}
