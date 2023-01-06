package carpet.mixins;

import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.FlatLevelSource;

@Mixin(FlatLevelSource.class) // YEET
public abstract class FlatLevelSource_structuresMixin extends ChunkGenerator
{
    public FlatLevelSource_structuresMixin(BiomeSource biomeSource) {
        super(biomeSource);
    }

/*
    @Override
    public WeightedRandomList<MobSpawnSettings.SpawnerData> getMobsAt(Holder<Biome> biome, StructureFeatureManager accessor, MobCategory group, BlockPos pos)
    {
        if (!CarpetSettings.flatWorldStructureSpawning) return super.getMobsAt(biome, accessor, group, pos);

        // vanila noise one
        if (accessor.getStructureAt(pos, StructureFeature.SWAMP_HUT).isValid()) {
            if (group == MobCategory.MONSTER) {
                return SwamplandHutFeature.SWAMPHUT_ENEMIES;
            }

            if (group == MobCategory.CREATURE) {
                return SwamplandHutFeature.SWAMPHUT_ANIMALS;
            }
        }

        if (group == MobCategory.MONSTER) {
            if (accessor.getStructureAt(pos, StructureFeature.PILLAGER_OUTPOST).isValid()) {
                return PillagerOutpostFeature.OUTPOST_ENEMIES;
            }

            if (accessor.getStructureAt(pos, StructureFeature.OCEAN_MONUMENT).isValid()) {
                return OceanMonumentFeature.MONUMENT_ENEMIES;
            }

            if (accessor.getStructureAt(pos, StructureFeature.NETHER_BRIDGE).isValid()) {
                return NetherFortressFeature.FORTRESS_ENEMIES;
            }
        }


        // carpet spawns
        if (group == MobCategory.MONSTER)
        {
            if (CarpetSettings.huskSpawningInTemples)
            {
                if (accessor.getStructureAt(pos, StructureFeature.DESERT_PYRAMID).isValid())
                {
                    return CustomSpawnLists.PYRAMID_SPAWNS;
                }
            }
            if (CarpetSettings.shulkerSpawningInEndCities)
            {
                if (accessor.getStructureAt(pos, StructureFeature.END_CITY).isValid())
                {
                    return CustomSpawnLists.SHULKER_SPAWNS;
                }
            }
            if (CarpetSettings.piglinsSpawningInBastions)
            {
                if (accessor.getStructureAt(pos, StructureFeature.BASTION_REMNANT).isValid())
                {
                    return CustomSpawnLists.BASTION_SPAWNS;
                }
            }
        }
        return (group == MobCategory.UNDERGROUND_WATER_CREATURE || group == MobCategory.AXOLOTLS) && accessor.getStructureAt(pos, StructureFeature.OCEAN_MONUMENT).isValid() ? MobSpawnSettings.EMPTY_MOB_LIST : super.getMobsAt(biome, accessor, group, pos);


    }
    
 */
}
