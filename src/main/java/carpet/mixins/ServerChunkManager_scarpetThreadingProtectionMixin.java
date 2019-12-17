package carpet.mixins;

import carpet.script.exception.InternalExpressionException;
import carpet.script.value.ThreadValue;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

@Mixin(ServerChunkManager.class)
public class ServerChunkManager_scarpetThreadingProtectionMixin
{
    @Redirect(method = "getChunk(IILnet/minecraft/world/chunk/ChunkStatus;Z)Lnet/minecraft/world/chunk/Chunk;", at = @At(
            value = "INVOKE",
            target = "Ljava/util/concurrent/CompletableFuture;join()Ljava/lang/Object;",
            ordinal = 0
    ))
    private Object checkJoiningThreads(CompletableFuture<Chunk> completableFuture)
    {
        synchronized (ThreadValue.waitingTreads)
        {
            if (ThreadValue.waitingTreads.containsKey(Thread.currentThread()))
                throw new InternalExpressionException("Cannot join/wait on world accessing thread");
            ThreadValue.waitingTreads.put(Thread.currentThread(), 1);
        }
        Chunk res = completableFuture.join();
        ThreadValue.waitingTreads.remove(Thread.currentThread());
        return res;

    }
}
