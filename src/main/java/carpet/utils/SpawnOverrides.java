package carpet.utils;

import carpet.CarpetSettings;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.StructureType;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

public class SpawnOverrides {
    final private static Map<Pair<MobCategory, ResourceKey<Structure>>, Pair<BooleanSupplier, StructureSpawnOverride>> carpetOverrides = new HashMap<>();

    static {
        addOverride(() -> CarpetSettings.huskSpawningInTemples, MobCategory.MONSTER, BuiltinStructures.DESERT_PYRAMID, StructureSpawnOverride.BoundingBoxType.STRUCTURE,
                WeightedRandomList.create(new MobSpawnSettings.SpawnerData(EntityType.HUSK, 1, 1, 1))
        );
        addOverride(() -> CarpetSettings.shulkerSpawningInEndCities, MobCategory.MONSTER, BuiltinStructures.END_CITY, StructureSpawnOverride.BoundingBoxType.PIECE,
                WeightedRandomList.create(new MobSpawnSettings.SpawnerData(EntityType.SHULKER, 10, 4, 4))
        );
        addOverride(() -> CarpetSettings.piglinsSpawningInBastions, MobCategory.MONSTER, BuiltinStructures.BASTION_REMNANT, StructureSpawnOverride.BoundingBoxType.PIECE,
                WeightedRandomList.create(
                        new MobSpawnSettings.SpawnerData(EntityType.PIGLIN_BRUTE, 5, 1, 2),
                        new MobSpawnSettings.SpawnerData(EntityType.PIGLIN, 10, 2, 4),
                        new MobSpawnSettings.SpawnerData(EntityType.HOGLIN, 2, 1, 2)
                )
        );

    }

    public static void addOverride(BooleanSupplier when, MobCategory cat, ResourceKey<Structure> poo,
                                   StructureSpawnOverride.BoundingBoxType type, WeightedRandomList<MobSpawnSettings.SpawnerData> spawns) {
        carpetOverrides.put(Pair.of(cat, poo), Pair.of(when, new StructureSpawnOverride(type, spawns)));
    }

    public static WeightedRandomList<MobSpawnSettings.SpawnerData> test(StructureManager structureFeatureManager, LongSet foo,
                                                                        MobCategory cat, Structure confExisting, BlockPos where) {
        ResourceLocation resource = structureFeatureManager.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(confExisting);
        ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE, resource);
        final Pair<BooleanSupplier, StructureSpawnOverride> spawnData = carpetOverrides.get(Pair.of(cat, key));
        if (spawnData == null || !spawnData.getKey().getAsBoolean()) return null;
        StructureSpawnOverride override = spawnData.getRight();
        if (override.boundingBox() == StructureSpawnOverride.BoundingBoxType.STRUCTURE) {
            if (structureFeatureManager.getStructureAt(where, confExisting).isValid())
                return override.spawns();
        } else {
            List<StructureStart> starts = new ArrayList<>(1);
            structureFeatureManager.fillStartsForStructure(confExisting, foo, starts::add);
            for (StructureStart start : starts) {
                if (start != null && start.isValid() && structureFeatureManager.structureHasPieceAt(where, start)) {
                    return override.spawns();
                }
            }
        }
        return null;
    }

    public static boolean isStructureAtPosition(ServerLevel level, ResourceKey<Structure> structureKey, BlockPos pos)
    {
        final Structure fortressFeature = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(structureKey);
        if (fortressFeature == null) {
            return false;
        }
        return level.structureManager().getStructureAt(pos, fortressFeature).isValid();
    }

    public static List<StructureStart> startsForFeature(ServerLevel level, SectionPos sectionPos, StructureType<?> structure) {
        Map<Structure, LongSet> allrefs = level.getChunk(sectionPos.x(), sectionPos.z(), ChunkStatus.STRUCTURE_REFERENCES).getAllReferences();
        List<StructureStart> result = new ArrayList<>();
        for (var entry: allrefs.entrySet())
        {
            Structure existing = entry.getKey();
            if (existing.type() == structure)
            {
                level.structureManager().fillStartsForStructure(existing, entry.getValue(), result::add);
            }
        }
        return result;
    }
}
