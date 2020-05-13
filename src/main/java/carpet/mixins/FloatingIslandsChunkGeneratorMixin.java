package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.class_5284;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.FloatingIslandsChunkGenerator;
import net.minecraft.world.gen.chunk.SurfaceChunkGenerator;
import net.minecraft.world.gen.feature.Feature;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(FloatingIslandsChunkGenerator.class)
public abstract class FloatingIslandsChunkGeneratorMixin extends SurfaceChunkGenerator<class_5284> //FloatingIslandsChunkGeneratorConfig
{
    public FloatingIslandsChunkGeneratorMixin(BiomeSource biomeSource, long l, class_5284 arg, int i, int j, int k, boolean bl)
    {
        super(biomeSource, l, arg, i, j, k, bl);
    }

    @Override
    public List<Biome.SpawnEntry> getEntitySpawnList(Biome biome, StructureAccessor structureAccessor, SpawnGroup category, BlockPos pos)
    {
        if (CarpetSettings.shulkerSpawningInEndCities && SpawnGroup.MONSTER == category)
        {
            if (Feature.END_CITY.isInsideStructure(structureAccessor, pos))
            {
                return Feature.END_CITY.getMonsterSpawns();
            }
        }
        return super.getEntitySpawnList(biome, structureAccessor, category, pos);
    }
}
