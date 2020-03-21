package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.FlatChunkGenerator;
import net.minecraft.world.gen.chunk.FlatChunkGeneratorConfig;
import net.minecraft.world.gen.feature.Feature;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(FlatChunkGenerator.class)
public abstract class FlatChunkGeneratorMixin extends ChunkGenerator<FlatChunkGeneratorConfig>
{
    public FlatChunkGeneratorMixin(IWorld iWorld, BiomeSource biome, FlatChunkGeneratorConfig chunkGeneratorConfig)
    {
        super(iWorld, biome, chunkGeneratorConfig);
    }
    
    @Override
    public List<Biome.SpawnEntry> getEntitySpawnList(EntityCategory category, BlockPos pos)
    {
        if (CarpetSettings.flatWorldStructureSpawning)
        {
            if (Feature.SWAMP_HUT.method_14029(this.world, pos))
            {
                if (category == EntityCategory.MONSTER)
                {
                    return Feature.SWAMP_HUT.getMonsterSpawns();
                }
        
                if (category == EntityCategory.CREATURE)
                {
                    return Feature.SWAMP_HUT.getCreatureSpawns();
                }
            }
            else if (category == EntityCategory.MONSTER)
            {
                if (Feature.PILLAGER_OUTPOST.isApproximatelyInsideStructure(this.world, pos))
                {
                    return Feature.PILLAGER_OUTPOST.getMonsterSpawns();
                }
    
                if (CarpetSettings.huskSpawningInTemples)
                {
                    if (Feature.DESERT_PYRAMID.isApproximatelyInsideStructure(this.world, pos))
                    {
                        return Feature.DESERT_PYRAMID.getMonsterSpawns();
                    }
                }
        
                if (Feature.OCEAN_MONUMENT.isApproximatelyInsideStructure(this.world, pos))
                {
                    return Feature.OCEAN_MONUMENT.getMonsterSpawns();
                }
    
                if (Feature.NETHER_BRIDGE.isInsideStructure(this.world, pos))
                {
                    return Feature.NETHER_BRIDGE.getMonsterSpawns();
                }
    
                if (Feature.NETHER_BRIDGE.isApproximatelyInsideStructure(this.world, pos) && this.world.getBlockState(pos.down(1)).getBlock() == Blocks.NETHER_BRICKS)
                {
                    return Feature.NETHER_BRIDGE.getMonsterSpawns();
                }
    
                if (CarpetSettings.shulkerSpawningInEndCities)
                {
                    if (Feature.END_CITY.isInsideStructure(this.world, pos))
                    {
                        return Feature.END_CITY.getMonsterSpawns();
                    }
                }
            }
        }
        
        return super.getEntitySpawnList(category, pos);
    }
}
