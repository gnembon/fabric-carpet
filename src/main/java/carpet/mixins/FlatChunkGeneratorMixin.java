package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.block.Blocks;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorConfig;
import net.minecraft.world.gen.chunk.FlatChunkGenerator;
import net.minecraft.world.gen.chunk.FlatChunkGeneratorConfig;
import net.minecraft.world.gen.feature.Feature;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(FlatChunkGenerator.class)
public abstract class FlatChunkGeneratorMixin extends ChunkGenerator
{
    public FlatChunkGeneratorMixin(BiomeSource biomeSource, ChunkGeneratorConfig chunkGeneratorConfig)
    {
        super(biomeSource, chunkGeneratorConfig);
    }
    
    @Override
    public List<Biome.SpawnEntry> getEntitySpawnList(Biome biome, StructureAccessor structureAccessor, SpawnGroup category, BlockPos blockPos)
    {
        if (CarpetSettings.flatWorldStructureSpawning)
        {
            if (Feature.SWAMP_HUT.isApproximatelyInsideStructure(structureAccessor, blockPos))
            {
                if (category == SpawnGroup.MONSTER)
                {
                    return Feature.SWAMP_HUT.getMonsterSpawns();
                }
        
                if (category == SpawnGroup.CREATURE)
                {
                    return Feature.SWAMP_HUT.getCreatureSpawns();
                }
            }
            else if (category == SpawnGroup.MONSTER)
            {
                if (Feature.PILLAGER_OUTPOST.isApproximatelyInsideStructure(structureAccessor, blockPos))
                {
                    return Feature.PILLAGER_OUTPOST.getMonsterSpawns();
                }
    
                if (CarpetSettings.huskSpawningInTemples)
                {
                    if (Feature.DESERT_PYRAMID.isApproximatelyInsideStructure(structureAccessor, blockPos))
                    {
                        return Feature.DESERT_PYRAMID.getMonsterSpawns();
                    }
                }
        
                if (Feature.OCEAN_MONUMENT.isApproximatelyInsideStructure(structureAccessor, blockPos))
                {
                    return Feature.OCEAN_MONUMENT.getMonsterSpawns();
                }
    
                if (Feature.NETHER_BRIDGE.isInsideStructure(structureAccessor, blockPos))
                {
                    return Feature.NETHER_BRIDGE.getMonsterSpawns();
                }
    
                if (CarpetSettings.shulkerSpawningInEndCities)
                {
                    if (Feature.END_CITY.isInsideStructure(structureAccessor, blockPos))
                    {
                        return Feature.END_CITY.getMonsterSpawns();
                    }
                }
            }
        }
        
        return super.getEntitySpawnList(biome, structureAccessor, category, blockPos);
    }
}
