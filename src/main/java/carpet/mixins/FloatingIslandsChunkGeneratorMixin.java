package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.class_5138;
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
    public List<Biome.SpawnEntry> getEntitySpawnList(class_5138 arg, EntityCategory category, BlockPos pos)
    {
        if (CarpetSettings.shulkerSpawningInEndCities && EntityCategory.MONSTER == category)
        {
            if (Feature.END_CITY.isInsideStructure(this.world, arg, pos))
            {
                return Feature.END_CITY.getMonsterSpawns();
            }
        }
        return this.world.getBiome(pos).getEntitySpawnList(category);
    }
}
