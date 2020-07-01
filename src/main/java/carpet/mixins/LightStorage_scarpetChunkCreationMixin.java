package carpet.mixins;

import java.util.Arrays;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import carpet.fakes.LightStorageInterface;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkToNibbleArrayMap;
import net.minecraft.world.chunk.light.ChunkLightProvider;
import net.minecraft.world.chunk.light.LightStorage;

@Mixin(LightStorage.class)
public abstract class LightStorage_scarpetChunkCreationMixin implements LightStorageInterface
{
    @Shadow
    protected abstract ChunkNibbleArray getLightArray(final long sectionPos, final boolean cached);

    @Shadow
    protected abstract void removeChunkData(final ChunkLightProvider<?, ?> storage, final long blockChunkPos);

    @Shadow
    @Final
    protected LongSet field_15802;

    @Shadow
    @Final
    protected ChunkToNibbleArrayMap<?> lightArrays;

    @Shadow protected abstract boolean hasLight(final long sectionPos);

    @Shadow
    @Final
    protected Long2ObjectMap<ChunkNibbleArray> lightArraysToAdd;

    @Shadow protected abstract void setLightEnabled(final long l, final boolean bl);

    @Unique
    private final LongSet removedChunks = new LongOpenHashSet();

    @Unique
    private final LongSet relightChunks = new LongOpenHashSet();

    @Override
    public void removeLightData(final long pos)
    {
        this.removedChunks.add(pos);
    }

    @Override
    public void relight(final long pos)
    {
        this.relightChunks.add(pos);
    }

    @Inject(
        method = "updateLightArrays(Lnet/minecraft/world/chunk/light/ChunkLightProvider;ZZ)V",
        at = @At("HEAD")
    )
    private void processData(final ChunkLightProvider<?, ?> lightProvider, final boolean doSkylight, final boolean skipEdgeLightPropagation, final CallbackInfo ci)
    {
        // Process light removal

        for (final long cPos : this.removedChunks)
        {
            for (int y = -1; y < 17; ++y)
            {
                final long sectionPos = ChunkSectionPos.asLong(ChunkSectionPos.getX(cPos), y, ChunkSectionPos.getZ(cPos));

                this.lightArraysToAdd.remove(sectionPos);

                if (this.hasLight(sectionPos))
                {
                    this.removeChunkData(lightProvider, sectionPos);

                    if (this.field_15802.add(sectionPos))
                        this.lightArrays.replaceWithCopy(sectionPos);

                    Arrays.fill(this.getLightArray(sectionPos, true).asByteArray(), (byte) 0);
                }
            }

            this.processRemoveLightData(cPos);
        }

        this.removedChunks.clear();

        // Process relighting

        for (final long cPos : this.relightChunks)
            this.processRelight(lightProvider, cPos);

        this.relightChunks.clear();
    }

    @Override
    public void processRemoveLightData(final long pos)
    {
    }

    @Override
    public void processRelight(final ChunkLightProvider<?, ?> lightProvider, final long pos)
    {
    }
}
