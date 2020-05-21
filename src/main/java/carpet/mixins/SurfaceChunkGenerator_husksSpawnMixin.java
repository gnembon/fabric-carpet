package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.class_5311;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.SurfaceChunkGenerator;
//import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.StructureFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(SurfaceChunkGenerator.class)
public abstract class SurfaceChunkGenerator_husksSpawnMixin extends ChunkGenerator
{

    public SurfaceChunkGenerator_husksSpawnMixin(BiomeSource biomeSource, class_5311 arg)
    {
        super(biomeSource, arg);
    }

    @Inject(method = "getEntitySpawnList", at = @At("HEAD"), cancellable = true)
    private void isInsidePyramid(Biome biome, StructureAccessor accessor, SpawnGroup group, BlockPos pos, CallbackInfoReturnable<List<Biome.SpawnEntry>> cir)
    {
        if (CarpetSettings.huskSpawningInTemples && group == SpawnGroup.MONSTER)
        {
            if (accessor.method_28388(pos, true, StructureFeature.DESERT_PYRAMID).hasChildren())
            {
                cir.setReturnValue(StructureFeature.DESERT_PYRAMID.getMonsterSpawns());
            }
        }
        if (CarpetSettings.shulkerSpawningInEndCities && SpawnGroup.MONSTER == group)
        {
            if (accessor.method_28388(pos, true, StructureFeature.END_CITY).hasChildren())
            {
                cir.setReturnValue(StructureFeature.END_CITY.getMonsterSpawns());
            }
        }
    }
}
