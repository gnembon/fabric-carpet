package carpet.mixins;

import carpet.settings.CarpetSettings;
import net.minecraft.entity.EntityCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.chunk.FloatingIslandsChunkGenerator;
import net.minecraft.world.gen.chunk.FloatingIslandsChunkGeneratorConfig;
import net.minecraft.world.gen.chunk.SurfaceChunkGenerator;
import net.minecraft.world.gen.feature.Feature;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(FloatingIslandsChunkGenerator.class)
public abstract class FloatingIslandsChunkGeneratorMixin extends SurfaceChunkGenerator<FloatingIslandsChunkGeneratorConfig>
{
    public FloatingIslandsChunkGeneratorMixin(IWorld iWorld_1, BiomeSource biomeSource_1, int int_1, int int_2, int int_3, FloatingIslandsChunkGeneratorConfig chunkGeneratorConfig_1, boolean boolean_1)
    {
        super(iWorld_1, biomeSource_1, int_1, int_2, int_3, chunkGeneratorConfig_1, boolean_1);
    }

    @Override
    public List<Biome.SpawnEntry> getEntitySpawnList(EntityCategory entityCategory_1, BlockPos blockPos_1)
    {
        if (CarpetSettings.shulkerSpawningInEndCities && EntityCategory.MONSTER == entityCategory_1)
        {
            if (Feature.END_CITY.isInsideStructure(this.world, blockPos_1))
            {
                return Feature.END_CITY.getMonsterSpawns();
            }
        }
        return this.world.getBiome(blockPos_1).getEntitySpawnList(entityCategory_1);
    }
}
