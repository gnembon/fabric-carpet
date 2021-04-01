package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.class_6012;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.chunk.StructuresConfig;
//import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.StructureFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(NoiseChunkGenerator.class)
// todo rename mixin after 1.15 is gone
public abstract class NoiseChunkGenerator_husksSpawnMixin extends ChunkGenerator
{
    public NoiseChunkGenerator_husksSpawnMixin(BiomeSource biomeSource, StructuresConfig structuresConfig)
    {
        super(biomeSource, structuresConfig);
    }

    @Inject(method = "getEntitySpawnList", at = @At("HEAD"), cancellable = true)
    private void isInsidePyramid(Biome biome, StructureAccessor accessor, SpawnGroup group, BlockPos pos, CallbackInfoReturnable<class_6012<SpawnSettings.SpawnEntry>> cir)
    {
        if (CarpetSettings.huskSpawningInTemples && group == SpawnGroup.MONSTER)
        {
            if (accessor.getStructureAt(pos, true, StructureFeature.DESERT_PYRAMID).hasChildren())
            {
                cir.setReturnValue(StructureFeature.DESERT_PYRAMID.getMonsterSpawns());
            }
        }
        if (CarpetSettings.shulkerSpawningInEndCities && SpawnGroup.MONSTER == group)
        {
            if (accessor.getStructureAt(pos, true, StructureFeature.END_CITY).hasChildren())
            {
                cir.setReturnValue(StructureFeature.END_CITY.getMonsterSpawns());
            }
        }
    }
}
