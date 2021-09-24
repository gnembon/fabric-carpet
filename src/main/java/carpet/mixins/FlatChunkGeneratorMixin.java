package carpet.mixins;

import carpet.CarpetSettings;
import carpet.helpers.CustomSpawnLists;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.FlatChunkGenerator;
import net.minecraft.world.gen.chunk.StructuresConfig;
import net.minecraft.world.gen.feature.NetherFortressFeature;
import net.minecraft.world.gen.feature.OceanMonumentFeature;
import net.minecraft.world.gen.feature.PillagerOutpostFeature;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.gen.feature.SwampHutFeature;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(FlatChunkGenerator.class)
public abstract class FlatChunkGeneratorMixin extends ChunkGenerator
{

    public FlatChunkGeneratorMixin(BiomeSource biomeSource, StructuresConfig structuresConfig)
    {
        super(biomeSource, structuresConfig);
    }

    @Override
    public Pool<SpawnSettings.SpawnEntry> getEntitySpawnList(Biome biome, StructureAccessor accessor, SpawnGroup group, BlockPos pos)
    {
        if (!CarpetSettings.flatWorldStructureSpawning) return super.getEntitySpawnList(biome, accessor, group, pos);

        // vanila noise one
        if (accessor.getStructureAt(pos, true, StructureFeature.SWAMP_HUT).hasChildren()) {
            if (group == SpawnGroup.MONSTER) {
                return SwampHutFeature.MONSTER_SPAWNS;
            }

            if (group == SpawnGroup.CREATURE) {
                return SwampHutFeature.CREATURE_SPAWNS;
            }
        }

        if (group == SpawnGroup.MONSTER) {
            if (accessor.getStructureAt(pos, false, StructureFeature.PILLAGER_OUTPOST).hasChildren()) {
                return PillagerOutpostFeature.MONSTER_SPAWNS;
            }

            if (accessor.getStructureAt(pos, false, StructureFeature.MONUMENT).hasChildren()) {
                return OceanMonumentFeature.MONSTER_SPAWNS;
            }

            if (accessor.getStructureAt(pos, true, StructureFeature.FORTRESS).hasChildren()) {
                return NetherFortressFeature.MONSTER_SPAWNS;
            }
        }


        // carpet spawns
        if (group == SpawnGroup.MONSTER)
        {
            if (CarpetSettings.huskSpawningInTemples)
            {
                if (accessor.getStructureAt(pos, true, StructureFeature.DESERT_PYRAMID).hasChildren())
                {
                    return CustomSpawnLists.PYRAMID_SPAWNS;
                }
            }
            if (CarpetSettings.shulkerSpawningInEndCities)
            {
                if (accessor.getStructureAt(pos, true, StructureFeature.END_CITY).hasChildren())
                {
                    return CustomSpawnLists.SHULKER_SPAWNS;
                }
            }
            if (CarpetSettings.piglinsSpawningInBastions)
            {
                if (accessor.getStructureAt(pos, true, StructureFeature.BASTION_REMNANT).hasChildren())
                {
                    return CustomSpawnLists.BASTION_SPAWNS;
                }
            }
        }
        return (group == SpawnGroup.UNDERGROUND_WATER_CREATURE || group == SpawnGroup.AXOLOTLS) && accessor.getStructureAt(pos, false, StructureFeature.MONUMENT).hasChildren() ? SpawnSettings.EMPTY_ENTRY_POOL : super.getEntitySpawnList(biome, accessor, group, pos);


    }
}
