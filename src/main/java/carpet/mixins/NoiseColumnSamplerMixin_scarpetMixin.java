package carpet.mixins;

import carpet.fakes.NoiseColumnSamplerInterface;
import carpet.script.exception.InternalExpressionException;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.util.math.noise.InterpolatedNoiseSampler;
import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.biome.source.util.TerrainNoisePoint;
import net.minecraft.world.gen.NoiseColumnSampler;
import net.minecraft.world.gen.NoiseHelper;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.GenerationShapeConfig;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(NoiseColumnSampler.class)
public abstract class NoiseColumnSamplerMixin_scarpetMixin implements NoiseColumnSamplerInterface {

    @Shadow @Final
    private InterpolatedNoiseSampler terrainNoise;

    @Shadow @Final @Nullable
    private SimplexNoiseSampler islandNoise;

    @Shadow @Final
    private DoublePerlinNoiseSampler aquiferBarrierNoise;

    @Shadow @Final
    private DoublePerlinNoiseSampler aquiferFluidLevelFloodednessNoise;

    @Shadow @Final
    private DoublePerlinNoiseSampler aquiferFluidLevelSpreadNoise;

    @Shadow @Final
    private DoublePerlinNoiseSampler aquiferLavaNoise;

    @Shadow @Final
    private DoublePerlinNoiseSampler pillarRarenessNoise;

    @Shadow @Final
    private DoublePerlinNoiseSampler pillarThicknessNoise;

    @Shadow @Final
    private DoublePerlinNoiseSampler spaghetti2dElevationNoise;

    @Shadow @Final
    private DoublePerlinNoiseSampler spaghetti2dModulatorNoise;

    @Shadow @Final
    private DoublePerlinNoiseSampler spaghetti2dThicknessNoise;

    @Shadow @Final
    private DoublePerlinNoiseSampler spaghetti3dFirstNoise;

    @Shadow @Final
    private DoublePerlinNoiseSampler spaghetti3dSecondNoise;

    @Shadow @Finalav
    private DoublePerlinNoiseSampler spaghetti3dRarityNoise;

    @Shadow @Final
    private DoublePerlinNoiseSampler spaghetti3dThicknessNoise;

    @Shadow @Final
    private DoublePerlinNoiseSampler spaghettiRoughnessModulatorNoise;

    @Shadow @Final
    private DoublePerlinNoiseSampler caveCheeseNoise;

    @Shadow @Final
    private DoublePerlinNoiseSampler oreGapNoise;

    @Shadow @Final
    private GenerationShapeConfig config;

    @Shadow
    protected abstract double method_38409(double d, double e, double f);

    @Shadow
    protected abstract double sampleCaveLayerNoise(int x, int y, int z);

    @Shadow
    protected abstract double samplePillarNoise(int x, int y, int z);

    @Shadow
    protected abstract double sampleSpaghetti2dNoise(int x, int y, int z);

    @Shadow
    protected abstract double sampleSpaghetti3dNoise(int x, int y, int z);

    @Shadow
    protected abstract double sampleSpaghettiRoughnessNoise(int x, int y, int z);

    @Shadow
    protected abstract double sampleCaveEntranceNoise(int x, int y, int z);

    @Shadow
    public abstract double sampleShiftNoise(int x, int y, int z);

    @Shadow
    private static double sample(DoublePerlinNoiseSampler sampler, double x, double y, double z, double invertedScale) {
        throw new AssertionError();
    }

    @Shadow
    public abstract MultiNoiseUtil.NoiseValuePoint sample(int i, int j, int k);

    @Shadow
    public abstract TerrainNoisePoint createTerrainNoisePoint(int x, int z, float continentalness, float weirdness, float erosion, Blender blender);

    @Override
    public double getNoiseSample(String name, int x, int y, int z) {
        MultiNoiseUtil.NoiseValuePoint noiseValuePoint = this.sample(x, y, z);
        TerrainNoisePoint terrainNoisePoint = createTerrainNoisePoint(
                x,
                z,
                MultiNoiseUtil.method_38666(noiseValuePoint.continentalnessNoise()),
                MultiNoiseUtil.method_38666(noiseValuePoint.weirdnessNoise()),
                MultiNoiseUtil.method_38666(noiseValuePoint.erosionNoise()),
                Blender.getNoBlending()
        );
        switch (name) {
            case "temperature" -> {
                return MultiNoiseUtil.method_38666(noiseValuePoint.temperatureNoise());
            }
            case "humidity" -> {
                return MultiNoiseUtil.method_38666(noiseValuePoint.humidityNoise());
            }
            case "continentalness" -> {
                return MultiNoiseUtil.method_38666(noiseValuePoint.continentalnessNoise());
            }
            case "erosion" -> {
                return MultiNoiseUtil.method_38666(noiseValuePoint.erosionNoise());
            }
            case "weirdness" -> {
                return MultiNoiseUtil.method_38666(noiseValuePoint.weirdnessNoise());
            }
            case "depth" -> {
                return MultiNoiseUtil.method_38666(noiseValuePoint.depth());
            }
            case "shiftX" -> {
                return x + this.sampleShiftNoise(x, 0, z);
            }
            case "shiftY" -> {
                return y + this.sampleShiftNoise(y, z, x);
            }
            case "shiftZ" -> {
                return z + this.sampleShiftNoise(z, x, 0);
            }
            case "terrain" -> {
                return this.terrainNoise.calculateNoise(x, y, z);
            }
            case "island" -> {
                return this.islandNoise == null ? 0 : this.islandNoise.sample(x, y, z);
            }
            case "jagged" -> {
                return this.method_38409(terrainNoisePoint.peaks(), x, z);
            }
            case "aquiferBarrier" -> {
                return this.aquiferBarrierNoise.sample(x, y, z);
            }
            case "aquiferFluidLevelFloodedness" -> {
                return this.aquiferFluidLevelFloodednessNoise.sample(x, y, z);
            }
            case "aquiferFluidLevelSpread" -> {
                return this.aquiferFluidLevelSpreadNoise.sample(x, y, z);
            }
            case "aquiferLava" -> {
                return this.aquiferLavaNoise.sample(x, y, z);
            }
            case "pillar" -> {
                return this.samplePillarNoise(x, y, z);
            }
            case "pillarRareness" -> {
                return NoiseHelper.lerpFromProgress(this.pillarRarenessNoise, x, y, z, 0.0D, 2.0D);
            }
            case "pillarThickness" -> {
                return NoiseHelper.lerpFromProgress(this.pillarThicknessNoise, x, y, z, 0.0D, 1.1D);
            }
            case "spaghetti2d" -> {
                return this.sampleSpaghetti2dNoise(x, y, z);
            }
            case "spaghetti2dElevation" -> {
                return NoiseHelper.lerpFromProgress(this.spaghetti2dElevationNoise, x, 0.0, z, this.config.minimumBlockY(), 8.0);
            }
            case "spaghetti2dModulator" -> {
                return this.spaghetti2dModulatorNoise.sample(x * 2, y, z * 2);
            }
            case "spaghetti2dThickness" -> {
                return NoiseHelper.lerpFromProgress(this.spaghetti2dThicknessNoise, x * 2, y, z * 2, 0.6, 1.3);
            }
            case "spaghetti3d" -> {
                return this.sampleSpaghetti3dNoise(x, y, z);
            }
            case "spaghetti3dFirst" -> {
                double d = this.spaghetti3dRarityNoise.sample(x * 2, y, z * 2);
                double e = CaveScalerMixin_scarpetMixin.invokeScaleTunnels(d);
                return sample(this.spaghetti3dFirstNoise, x, y, z, e);
            }
            case "spaghetti3dSecond" -> {
                double d = this.spaghetti3dRarityNoise.sample(x * 2, y, z * 2);
                double e = CaveScalerMixin_scarpetMixin.invokeScaleTunnels(d);
                return sample(this.spaghetti3dSecondNoise, x, y, z, e);
            }
            case "spaghetti3dRarity" -> {
                return this.spaghetti3dRarityNoise.sample(x * 2, y, z * 2);
            }
            case "spaghetti3dThickness" -> {
                return NoiseHelper.lerpFromProgress(this.spaghetti3dThicknessNoise, x, y, z, 0.065, 0.088);
            }
            case "spaghettiRoughness" -> {
                return this.sampleSpaghettiRoughnessNoise(x, y, z);
            }
            case "spaghettiRoughnessModulator" -> {
                return NoiseHelper.lerpFromProgress(this.spaghettiRoughnessModulatorNoise, x, y, z, 0.0, 0.1);
            }
            case "caveEntrance" -> {
                return this.sampleCaveEntranceNoise(
                        BiomeCoords.toBlock(x),
                        BiomeCoords.toBlock(y),
                        BiomeCoords.toBlock(z)
                );
            }
            case "caveLayer" -> {
                // scaling them by 4 because it's sampled in normal coords
                return this.sampleCaveLayerNoise(
                        BiomeCoords.toBlock(x),
                        BiomeCoords.toBlock(y),
                        BiomeCoords.toBlock(z)
                );
            }
            case "caveCheese" -> {
                // same reason as above
                return this.caveCheeseNoise.sample(
                        BiomeCoords.toBlock(x),
                        BiomeCoords.toBlock(y) / 1.5,
                        BiomeCoords.toBlock(z)
                );
            }
            case "terrainPeaks" -> {
                return terrainNoisePoint.peaks();
            }
            case "terrainOffset" -> {
                return terrainNoisePoint.offset();
            }
            case "terrainFactor" -> {
                return terrainNoisePoint.factor();
            }
            case "oreGap" -> {
                return this.oreGapNoise.sample(x, y, z);
            }
        }
        throw new InternalExpressionException("Unknown noise type: " + name);
    }
}
