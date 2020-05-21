package carpet.mixins;

import carpet.CarpetSettings;
//import net.minecraft.block.Blocks;
import net.minecraft.class_5311;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.util.math.BlockPos;
//import net.minecraft.world.WorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
//import net.minecraft.world.gen.chunk.ChunkGeneratorConfig;
import net.minecraft.world.gen.chunk.FlatChunkGenerator;
//import net.minecraft.world.gen.chunk.FlatChunkGeneratorConfig;
//import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.StructureFeature;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(FlatChunkGenerator.class)
public abstract class FlatChunkGeneratorMixin extends ChunkGenerator
{

    public FlatChunkGeneratorMixin(BiomeSource biomeSource, class_5311 arg)
    {
        super(biomeSource, arg);
    }

    @Override
    public List<Biome.SpawnEntry> getEntitySpawnList(Biome biome, StructureAccessor accessor, SpawnGroup group, BlockPos pos)
    {
        if (CarpetSettings.flatWorldStructureSpawning)
        {
            if (accessor.method_28388(pos, true, StructureFeature.field_24851).hasChildren())
            {  //swamp hut
                if (group == SpawnGroup.MONSTER)
                {
                    return StructureFeature.field_24851.getMonsterSpawns();
                }

                if (group == SpawnGroup.CREATURE)
                {
                    return StructureFeature.field_24851.getCreatureSpawns();
                }
            }

            if (group == SpawnGroup.MONSTER)
            {
                if (accessor.method_28388(pos, false, StructureFeature.PILLAGER_OUTPOST).hasChildren())
                {
                    return StructureFeature.PILLAGER_OUTPOST.getMonsterSpawns();
                }

                if (CarpetSettings.huskSpawningInTemples)
                {
                    if (accessor.method_28388(pos, true, StructureFeature.DESERT_PYRAMID).hasChildren())
                    {
                        return StructureFeature.DESERT_PYRAMID.getMonsterSpawns();
                    }
                }

                if (accessor.method_28388(pos, false, StructureFeature.MONUMENT).hasChildren())
                {
                    return StructureFeature.MONUMENT.getMonsterSpawns();
                }

                if (accessor.method_28388(pos, true, StructureFeature.FORTRESS).hasChildren())
                {
                    return StructureFeature.FORTRESS.getMonsterSpawns();
                }

                if (CarpetSettings.shulkerSpawningInEndCities)
                {
                    if (accessor.method_28388(pos, true, StructureFeature.END_CITY).hasChildren())
                    {
                        return StructureFeature.END_CITY.getMonsterSpawns();
                    }
                }
            }
        }
        return super.getEntitySpawnList(biome, accessor, group, pos);
    }
}
