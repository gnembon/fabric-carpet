package carpet.mixins;

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
public abstract class NoiseColumnSamplerMixin_scarpetMixin {

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

    public double queryNoiseSample$terrainNoise(int x, int y, int z) {
        return this.terrainNoise.calculateNoise(x, y, z);
    }

    public double queryNoiseSample$islandNoise(int x, int y, int z) {
        return this.islandNoise == null ? 0 : this.islandNoise.sample(x, y, z);
    }

    public double queryNoiseSample$jaggedNoise(int x, int y, int z) {
        return this.method_38409(x, y, z);
    }

    public double queryNoiseSample$aquiferBarrierNoise(int x, int y, int z) {
        return this.aquiferBarrierNoise.sample(x, y, z);
    }

    public double queryNoiseSample$aquiferFluidLevelFloodednessNoise(int x, int y, int z) {
        return this.aquiferFluidLevelFloodednessNoise.sample(x, y, z);
    }

    public double queryNoiseSample$aquiferFluidLevelSpreadNoise(int x, int y, int z) {
        return this.aquiferFluidLevelSpreadNoise.sample(x, y, z);
    }

    public double queryNoiseSample$aquiferLavaNoise(int x, int y, int z) {
        return this.aquiferLavaNoise.sample(x, y, z);
    }

    public double queryNoiseSample$caveLayerNoise(int x, int y, int z) {
        return this.sampleCaveLayerNoise(x, y, z);
    }

    public double queryNoiseSample$pillarNoise(int x, int y, int z) {
        return this.samplePillarNoise(x, y, z);
    }

    public double queryNoiseSample$pillarRarenessNoise(int x, int y, int z) {
        return NoiseHelper.lerpFromProgress(this.pillarRarenessNoise, x, y, z, 0.0D, 2.0D);
    }

    public double queryNoiseSample$pillarThicknessNoise(int x, int y, int z) {
        return NoiseHelper.lerpFromProgress(this.pillarThicknessNoise, x, y, z, 0.0D, 1.1D);
    }

    public double queryNoiseSample$spaghetti2dNoise(int x, int y, int z) {
        return this.sampleSpaghetti2dNoise(x, y, z);
    }

    public double queryNoiseSample$spaghetti2dElevationNoise(int x, int y, int z) {
        return NoiseHelper.lerpFromProgress(this.spaghetti2dElevationNoise, (double) x, 0.0, (double) z, (double) this.config.method_39548(), 8.0);
    }

    public double queryNoiseSample$spaghetti2dModulatorNoise(int x, int y, int z) {
        return this.spaghetti2dModulatorNoise.sample((double) (x * 2), (double) y, (double) (z * 2));
    }

    public double queryNoiseSample$spaghetti2dThicknessNoise(int x, int y, int z) {
        return NoiseHelper.lerpFromProgress(this.spaghetti2dThicknessNoise, (double) (x * 2), (double) y, (double) (z * 2), 0.6, 1.3);
    }

    public double queryNoiseSample$spaghetti3dNoise(int x, int y, int z) {
        return this.sampleSpaghetti3dNoise(x, y, z);
    }

    public double queryNoiseSample$spaghetti3dFirstNoise(int x, int y, int z) {
        double d = this.spaghetti3dRarityNoise.sample(x * 2, y, z * 2);
        double e = CaveScalerMixin_scarpetMixin.invokeScaleTunnels(d);
        return sample(this.spaghetti3dFirstNoise, x, y, z, e);
    }

    public double queryNoiseSample$spaghetti3dSecondNoise(int x, int y, int z) {
        double d = this.spaghetti3dRarityNoise.sample(x * 2, y, z * 2);
        double e = CaveScalerMixin_scarpetMixin.invokeScaleTunnels(d);
        return sample(this.spaghetti3dSecondNoise, x, y, z, e);
    }

    public double queryNoiseSample$spaghetti3dRarityNoise(int x, int y, int z) {
        return this.spaghetti3dRarityNoise.sample(x * 2, y, z * 2);
    }

    public double queryNoiseSample$spaghetti3dThicknessNoise(int x, int y, int z) {
        return NoiseHelper.lerpFromProgress(this.spaghetti3dThicknessNoise, x, y, z, 0.065, 0.088);
    }

    public double queryNoiseSample$spaghettiRoughnessNoise(int x, int y, int z) {
        return this.sampleSpaghettiRoughnessNoise(x, y, z);
    }

    public double queryNoiseSample$spaghettiRoughnessModulatorNoise(int x, int y, int z) {
        return NoiseHelper.lerpFromProgress(this.spaghettiRoughnessModulatorNoise, x, y, z, 0.0, 0.1);
    }

    public double queryNoiseSample$caveEntranceNoise(int x, int y, int z) {
        return this.sampleCaveEntranceNoise(x, y, z);
    }

    public double queryNoiseSample$caveCheeseNoise(int x, int y, int z) {
        return this.caveCheeseNoise.sample(x, y / 1.5, z);
    }

    public double queryNoiseSample$temperatureNoise(int x, int y, int z) {
        return this.sampleTemperatureNoise(x, y, z);
    }

    public double queryNoiseSample$humidityNoise(int x, int y, int z) {
        return this.sampleHumidityNoise(x, y, z);
    }

    public double queryNoiseSample$continentalnessNoise(int x, int y, int z) {
        return this.sampleContinentalnessNoise(x, y, z);
    }

    public double queryNoiseSample$erosionNoise(int x, int y, int z) {
        return this.sampleErosionNoise(x, y, z);
    }

    public double queryNoiseSample$weirdnessNoise(int x, int y, int z) {
        return this.sampleWeirdnessNoise(x, y, z);
    }

    public double queryNoiseSample$depthNoise(int x, int y, int z) {
        return this.sample(x, y, z).depth();
    }

    public double queryNoiseSample$shiftNoise(int x, int y, int z) {
        return this.sampleShiftNoise(x, y, z);
    }

    public double queryNoiseSample$oreGapNoise(int x, int y, int z) {
        return this.oreGapNoise.sample(x, y, z);
    }
}
