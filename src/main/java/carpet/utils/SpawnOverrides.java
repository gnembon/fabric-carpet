package carpet.utils;

import carpet.CarpetSettings;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.data.worldgen.StructureFeatures;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

public class SpawnOverrides {
    final private static Map<Pair<MobCategory, StructureFeature<?>>, Pair<BooleanSupplier, StructureSpawnOverride>> carpetOverrides = new HashMap<>();
    static {
        addOverride(() -> CarpetSettings.huskSpawningInTemples, MobCategory.MONSTER, StructureFeatures.DESERT_PYRAMID, StructureSpawnOverride.BoundingBoxType.STRUCTURE,
                        WeightedRandomList.create(new MobSpawnSettings.SpawnerData(EntityType.HUSK, 1, 1, 1))
        );
        addOverride(() -> CarpetSettings.shulkerSpawningInEndCities, MobCategory.MONSTER, StructureFeatures.END_CITY, StructureSpawnOverride.BoundingBoxType.PIECE,
                WeightedRandomList.create(new MobSpawnSettings.SpawnerData(EntityType.SHULKER, 10, 4, 4))
        );
        addOverride(() -> CarpetSettings.piglinsSpawningInBastions, MobCategory.MONSTER, StructureFeatures.BASTION_REMNANT, StructureSpawnOverride.BoundingBoxType.PIECE,
                WeightedRandomList.create(
                        new MobSpawnSettings.SpawnerData(EntityType.PIGLIN_BRUTE, 5, 1, 2),
                        new MobSpawnSettings.SpawnerData(EntityType.PIGLIN, 10, 2, 4),
                        new MobSpawnSettings.SpawnerData(EntityType.HOGLIN, 2, 1, 2)
                )
        );

    }

    public static void addOverride(BooleanSupplier when, MobCategory cat, Holder<ConfiguredStructureFeature<?,?>> poo,
                                   StructureSpawnOverride.BoundingBoxType type, WeightedRandomList<MobSpawnSettings.SpawnerData> spawns)
    {
        carpetOverrides.put(Pair.of(cat, poo.value().feature), Pair.of(when, new StructureSpawnOverride(type, spawns)));
    }

    public static WeightedRandomList<MobSpawnSettings.SpawnerData> test(StructureFeatureManager structureFeatureManager, LongSet foo,
            MobCategory cat, ConfiguredStructureFeature<?,?> confExisting, BlockPos where)
    {
        final Pair<BooleanSupplier, StructureSpawnOverride> spawnData = carpetOverrides.get(Pair.of(cat, confExisting.feature));
        if (spawnData == null || !spawnData.getKey().getAsBoolean()) return null;
        StructureSpawnOverride override = spawnData.getRight();
        if (override.boundingBox() == StructureSpawnOverride.BoundingBoxType.STRUCTURE)
        {
            if (structureFeatureManager.getStructureAt(where, confExisting).isValid())
                return override.spawns();
        }
        else
        {
            List<StructureStart> starts = new ArrayList<>(1);
            structureFeatureManager.fillStartsForFeature(confExisting, foo, starts::add);
            for (StructureStart start: starts)
            {
                if (start != null && start.isValid() && structureFeatureManager.structureHasPieceAt(where, start)) {
                    return override.spawns();
                }
            }
        }
        return null;
    }
}
