package carpet.mixins;

import carpet.CarpetSettings;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.Holder;
import net.minecraft.data.worldgen.StructureFeatures;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;
import java.util.Map;

@Mixin(ChunkGenerator.class)
public abstract class ChunkGenerator_customMobSpawnsMixin
{

    @Shadow protected abstract void ensureGenerated();

    private static final WeightedRandomList<MobSpawnSettings.SpawnerData> PYRAMID_SPAWNS = WeightedRandomList.create(new MobSpawnSettings.SpawnerData(EntityType.HUSK, 1, 1, 1));
    private static final WeightedRandomList<MobSpawnSettings.SpawnerData> SHULKER_SPAWNS = WeightedRandomList.create(new MobSpawnSettings.SpawnerData(EntityType.SHULKER, 10, 4, 4));
    private static final WeightedRandomList<MobSpawnSettings.SpawnerData> BASTION_SPAWNS = WeightedRandomList.create(
            new MobSpawnSettings.SpawnerData(EntityType.PIGLIN_BRUTE, 5, 1, 2),
            new MobSpawnSettings.SpawnerData(EntityType.PIGLIN, 10, 2, 4),
            new MobSpawnSettings.SpawnerData(EntityType.HOGLIN, 2, 1, 2)
    );

    @Inject(
            method = "getMobsAt", locals = LocalCapture.CAPTURE_FAILHARD,
            at = @At(
                    value = "INVOKE",
                   target = "Ljava/util/Map$Entry;getKey()Ljava/lang/Object;"
            ), cancellable = true)
    private void isInsidePyramid(Holder<Biome> holder, StructureFeatureManager structureFeatureManager, MobCategory mobCategory, BlockPos blockPos,
                                 CallbackInfoReturnable<WeightedRandomList<MobSpawnSettings.SpawnerData>> cir,
                                 Map<ConfiguredStructureFeature<?, ?>, LongSet> map, Iterator<?> var6, Map.Entry<ConfiguredStructureFeature<?, ?>, LongSet> entry)
    {
        if (mobCategory == MobCategory.MONSTER)
        {
            ConfiguredStructureFeature<?, ?> str = entry.getKey();
            if (CarpetSettings.huskSpawningInTemples
                    && str.feature == StructureFeatures.DESERT_PYRAMID.value().feature
                    && structureFeatureManager.getStructureAt(blockPos, StructureFeatures.DESERT_PYRAMID.value()).isValid()) {
                cir.setReturnValue(PYRAMID_SPAWNS);
                return;
            }
            if (CarpetSettings.shulkerSpawningInEndCities
                    && str.feature == StructureFeatures.END_CITY.value().feature
                    && structureFeatureManager.getStructureAt(blockPos, StructureFeatures.END_CITY.value()).isValid()) {
                cir.setReturnValue(SHULKER_SPAWNS);
                return;
            }
            if (CarpetSettings.piglinsSpawningInBastions
                    && str.feature == StructureFeatures.BASTION_REMNANT.value().feature
                    && structureFeatureManager.getStructureAt(blockPos, StructureFeatures.BASTION_REMNANT.value()).isValid()) {
                cir.setReturnValue(BASTION_SPAWNS);
            }
        }
    }
}
