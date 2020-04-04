package carpet.mixins;

import carpet.fakes.ServerLightingProviderInterface;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Thread.yield;

@Mixin(ServerLightingProvider.class)
public abstract class ServerLightingProvider_scarpetChunkCreationMixin implements ServerLightingProviderInterface
{

    @Shadow protected abstract void updateChunkStatus(ChunkPos pos);

    @Shadow private volatile int taskBatchSize;

    @Shadow public abstract void setTaskBatchSize(int taskBatchSize);

    @Shadow @Final private AtomicBoolean field_18812;

    @Shadow @Final private ObjectList pendingTasks;

    @Shadow protected abstract void runTasks();

    @Override
    public void publicUpdateChunkStatus(ChunkPos pos)
    {
        updateChunkStatus(pos);
    }

    @Override
    public void flush()
    {
        try
        {
            while (!field_18812.compareAndSet(false, true)) yield();
            if (pendingTasks.size() == 0) return;
            int prevTaskBatchSize = taskBatchSize;
            setTaskBatchSize(pendingTasks.size());
            runTasks();
            setTaskBatchSize(prevTaskBatchSize);
        }
        finally
        {
            this.field_18812.set(false);
        }
    }
}
