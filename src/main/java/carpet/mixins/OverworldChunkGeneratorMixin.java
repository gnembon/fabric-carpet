package carpet.mixins;

import carpet.settings.CarpetSettings;
import net.minecraft.entity.EntityCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.chunk.OverworldChunkGenerator;
import net.minecraft.world.gen.chunk.OverworldChunkGeneratorConfig;
import net.minecraft.world.gen.chunk.SurfaceChunkGenerator;
import net.minecraft.world.gen.feature.Feature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(OverworldChunkGenerator.class)
public abstract class OverworldChunkGeneratorMixin extends SurfaceChunkGenerator<OverworldChunkGeneratorConfig>
{
    public OverworldChunkGeneratorMixin(IWorld iWorld_1, BiomeSource biomeSource_1, int int_1, int int_2, int int_3, OverworldChunkGeneratorConfig chunkGeneratorConfig_1, boolean boolean_1)
    {
        super(iWorld_1, biomeSource_1, int_1, int_2, int_3, chunkGeneratorConfig_1, boolean_1);
    }
    
    @Inject(method = "getEntitySpawnList", at = @At(value = "INVOKE", ordinal = 1, shift = At.Shift.BEFORE,
            target = "Lnet/minecraft/world/gen/feature/StructureFeature;isApproximatelyInsideStructure(Lnet/minecraft/world/IWorld;Lnet/minecraft/util/math/BlockPos;)Z"),
            cancellable = true)
    private void onGetEntitySpawnList(EntityCategory entityCategory_1, BlockPos blockPos_1,
            CallbackInfoReturnable<List<Biome.SpawnEntry>> cir)
    {
        if (CarpetSettings.huskSpawningInTemples)
        {
            if (Feature.DESERT_PYRAMID.isApproximatelyInsideStructure(this.world, blockPos_1))
            {
                cir.setReturnValue(Feature.DESERT_PYRAMID.getMonsterSpawns());
            }
        }
    }
}
