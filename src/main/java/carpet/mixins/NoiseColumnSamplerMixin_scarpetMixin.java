package carpet.mixins;

import carpet.fakes.NoiseColumnSamplerInterface;
import carpet.script.exception.InternalExpressionException;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.util.math.noise.InterpolatedNoiseSampler;
import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.gen.NoiseColumnSampler;
import net.minecraft.world.gen.NoiseHelper;
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

    @Shadow @Final
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
    protected abstract double sampleTemperatureNoise(double x, double y, double z);

    @Shadow
    protected abstract double sampleHumidityNoise(double x, double y, double z);

    @Shadow
    public abstract double sampleContinentalnessNoise(double x, double y, double z);

    @Shadow
    public abstract double sampleErosionNoise(double x, double y, double z);

    @Shadow
    public abstract double sampleWeirdnessNoise(double x, double y, double z);

    @Shadow
    public abstract double sampleShiftNoise(int x, int y, int z);

    @Shadow
    private static double sample(DoublePerlinNoiseSampler sampler, double x, double y, double z, double invertedScale) {
        throw new AssertionError();
    }

    @Shadow public abstract MultiNoiseUtil.NoiseValuePoint sample(int i, int j, int k);

    @Override
    public double getNoiseSample(String name, int x, int y, int z) {
        switch (name) {
            case "terrain" -> {
                return this.terrainNoise.calculateNoise(x, y, z);
            }
            case "island" -> {
                return this.islandNoise == null ? 0 : this.islandNoise.sample(x, y, z);
            }
            case "jagged" -> {
                return this.method_38409(x, y, z);
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
            case "caveLayer" -> {
                return this.sampleCaveLayerNoise(x, y, z);
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
                return NoiseHelper.lerpFromProgress(this.spaghetti2dElevationNoise, x, 0.0, z, this.config.method_39548(), 8.0);
            }
            case "spaghetti2dModulator" -> {
                return this.spaghetti2dModulatorNoise.sample(x * 2, y, z * 2);
            }
            case "spaghetti2dThickness" -> {
                return NoiseHelper.lerpFromProgress(this.spaghetti2dThicknessNoise, (double) (x * 2), (double) y, (double) (z * 2), 0.6, 1.3);
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
                return this.sampleCaveEntranceNoise(x, y, z);
            }
            case "caveCheese" -> {
                return this.caveCheeseNoise.sample(x, y / 1.5, z);
            }
            case "temperature" -> {
                return this.sampleTemperatureNoise(x, y, z);
            }
            case "humidity" -> {
                return this.sampleHumidityNoise(x, y, z);
            }
            case "continentalness" -> {
                return this.sampleContinentalnessNoise(x, y, z);
            }
            case "erosion" -> {
                return this.sampleErosionNoise(x, y, z);
            }
            case "weirdness" -> {
                return this.sampleWeirdnessNoise(x, y, z);
            }
            case "depth" -> {
                return this.sample(x, y, z).depth() / 10000.0F;
            }
            case "shift" -> {
                return this.sampleShiftNoise(x, y, z);
            }
            case "oreGap" -> {
                return this.oreGapNoise.sample(x, y, z);
            }
        }
        throw new InternalExpressionException("Unknown noise type: " + name);
    }
}
