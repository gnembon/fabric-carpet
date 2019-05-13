package carpet.mixins;

import carpet.CarpetSettings;
import carpet.utils.SpawnReporter;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.entity.EntityCategory;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.LevelProperties;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ServerChunkManager.class)
public abstract class ServerChunkManagerMixin
{
    @Shadow @Final private ServerWorld world;

    @Inject(
            method = "tickChunks",
            locals = LocalCapture.CAPTURE_FAILHARD,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/world/ThreadedAnvilChunkStorage;entryIterator()Lit/unimi/dsi/fastutil/objects/ObjectBidirectionalIterator;",
                    shift = At.Shift.AFTER,
                    ordinal = 0

    ))
    private void grabMobcaps(CallbackInfo ci,
                             long long_1,
                             long long_2,
                             LevelProperties levelProperties_1,
                             boolean boolean_1,
                             boolean boolean_2,
                             int int_1,
                             BlockPos blockPos_1,
                             boolean boolean_3,
                             int int_2,
                             EntityCategory[] entityCategorys_1,
                             Object2IntMap object2IntMap_1)
    {
        DimensionType dim = this.world.dimension.getType();
        SpawnReporter.mobCounts.put(dim, (Object2IntMap<EntityCategory>)object2IntMap_1);
        SpawnReporter.chunkCounts.put(dim, int_2);
    }

}
