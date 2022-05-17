package carpet.mixins;

import carpet.utils.SpawnOverrides;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.Holder;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;
import java.util.Map;

@Mixin(ChunkGenerator.class)
public abstract class ChunkGenerator_customMobSpawnsMixin
{
    @Inject(
            method = "getMobsAt", locals = LocalCapture.CAPTURE_FAILHARD,
            at = @At(
                    value = "INVOKE",
                   target = "Ljava/util/Map$Entry;getKey()Ljava/lang/Object;"
            ), cancellable = true)
    private void checkCMSpawns(Holder<Biome> holder, StructureManager structureFeatureManager, MobCategory mobCategory, BlockPos blockPos,
                               CallbackInfoReturnable<WeightedRandomList<MobSpawnSettings.SpawnerData>> cir,
                               Map<Structure, LongSet> map, Iterator<?> var6, Map.Entry<Structure, LongSet> entry)
    {
        WeightedRandomList<MobSpawnSettings.SpawnerData> res = SpawnOverrides.test(structureFeatureManager, entry.getValue(), mobCategory, entry.getKey(), blockPos);
        if (res != null)
        {
            cir.setReturnValue(res);
        }
    }
}
