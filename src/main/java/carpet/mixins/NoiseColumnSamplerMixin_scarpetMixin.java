package carpet.mixins;

import carpet.fakes.NoiseColumnSamplerInterface;
import carpet.script.exception.InternalExpressionException;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.NoiseSampler;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.TerrainInfo;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.NoiseUtils;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(NoiseSampler.class)
public abstract class NoiseColumnSamplerMixin_scarpetMixin implements NoiseColumnSamplerInterface {

    @Shadow @Final
    private BlendedNoise blendedNoise;

    @Shadow @Final @Nullable
    private SimplexNoise islandNoise;

    @Shadow @Final
    private NormalNoise barrierNoise;

    @Shadow @Final
    private NormalNoise fluidLevelFloodednessNoise;

    @Shadow @Final
    private NormalNoise fluidLevelSpreadNoise;

    @Shadow @Final
    private NormalNoise lavaNoise;

    @Shadow @Final
    private NormalNoise pillarRarenessModulator;

    @Shadow @Final
    private NormalNoise pillarThicknessModulator;

    @Shadow @Final
    private NormalNoise spaghetti2DElevationModulator;

    @Shadow @Final
    private NormalNoise spaghetti2DRarityModulator;

    @Shadow @Final
    private NormalNoise spaghetti2DThicknessModulator;

    @Shadow @Final
    private NormalNoise spaghetti3DNoiseSource1;

    @Shadow @Final
    private NormalNoise spaghetti3DNoiseSource2;

    @Shadow @Final
    private NormalNoise spaghetti3DRarityModulator;

    @Shadow @Final
    private NormalNoise spaghetti3DThicknessModulator;

    @Shadow @Final
    private NormalNoise spaghettiRoughnessModulator;

    @Shadow @Final
    private NormalNoise cheeseNoiseSource;

    @Shadow @Final
    private NormalNoise gapNoise;

    @Shadow @Final
    private NoiseSettings noiseSettings;

    @Shadow
    protected abstract double sampleJaggedNoise(double d, double e, double f);

    @Shadow
    protected abstract double getLayerizedCaverns(int x, int y, int z);

    @Shadow
    protected abstract double getPillars(int x, int y, int z);

    @Shadow
    protected abstract double getSpaghetti2D(int x, int y, int z);

    @Shadow
    protected abstract double getSpaghetti3D(int x, int y, int z);

    @Shadow
    protected abstract double spaghettiRoughness(int x, int y, int z);

    @Shadow
    protected abstract double getBigEntrances(int x, int y, int z);

    @Shadow
    public abstract double getOffset(int x, int y, int z);

    @Shadow
    private static double sampleWithRarity(NormalNoise sampler, double x, double y, double z, double invertedScale) {
        throw new AssertionError();
    }

    @Shadow
    public abstract Climate.TargetPoint sample(int i, int j, int k);

    @Shadow
    public abstract TerrainInfo terrainInfo(int x, int z, float continentalness, float weirdness, float erosion, Blender blender);

    @Override
    public double getNoiseSample(String name, int x, int y, int z) {
        Climate.TargetPoint noiseValuePoint = this.sample(x, y, z);
        TerrainInfo terrainNoisePoint = terrainInfo(
                x,
                z,
                Climate.unquantizeCoord(noiseValuePoint.continentalness()),
                Climate.unquantizeCoord(noiseValuePoint.weirdness()),
                Climate.unquantizeCoord(noiseValuePoint.erosion()),
                Blender.empty()
        );
        switch (name) {
            case "temperature" -> {
                return Climate.unquantizeCoord(noiseValuePoint.temperature());
            }
            case "humidity" -> {
                return Climate.unquantizeCoord(noiseValuePoint.humidity());
            }
            case "continentalness" -> {
                return Climate.unquantizeCoord(noiseValuePoint.continentalness());
            }
            case "erosion" -> {
                return Climate.unquantizeCoord(noiseValuePoint.erosion());
            }
            case "weirdness" -> {
                return Climate.unquantizeCoord(noiseValuePoint.weirdness());
            }
            case "depth" -> {
                return Climate.unquantizeCoord(noiseValuePoint.depth());
            }
            case "shiftX" -> {
                return x + this.getOffset(x, 0, z);
            }
            case "shiftY" -> {
                return y + this.getOffset(y, z, x);
            }
            case "shiftZ" -> {
                return z + this.getOffset(z, x, 0);
            }
            case "terrain" -> {
                return this.blendedNoise.calculateNoise(x, y, z);
            }
            case "island" -> {
                return this.islandNoise == null ? 0 : this.islandNoise.getValue(x, y, z);
            }
            case "jagged" -> {
                return this.sampleJaggedNoise(terrainNoisePoint.jaggedness(), x, z);
            }
            case "aquiferBarrier" -> {
                return this.barrierNoise.getValue(x, y, z);
            }
            case "aquiferFluidLevelFloodedness" -> {
                return this.fluidLevelFloodednessNoise.getValue(x, y, z);
            }
            case "aquiferFluidLevelSpread" -> {
                return this.fluidLevelSpreadNoise.getValue(x, y, z);
            }
            case "aquiferLava" -> {
                return this.lavaNoise.getValue(x, y, z);
            }
            case "pillar" -> {
                return this.getPillars(x, y, z);
            }
            case "pillarRareness" -> {
                return NoiseUtils.sampleNoiseAndMapToRange(this.pillarRarenessModulator, x, y, z, 0.0D, 2.0D);
            }
            case "pillarThickness" -> {
                return NoiseUtils.sampleNoiseAndMapToRange(this.pillarThicknessModulator, x, y, z, 0.0D, 1.1D);
            }
            case "spaghetti2d" -> {
                return this.getSpaghetti2D(x, y, z);
            }
            case "spaghetti2dElevation" -> {
                return NoiseUtils.sampleNoiseAndMapToRange(this.spaghetti2DElevationModulator, x, 0.0, z, this.noiseSettings.getMinCellY(), 8.0);
            }
            case "spaghetti2dModulator" -> {
                return this.spaghetti2DRarityModulator.getValue(x * 2, y, z * 2);
            }
            case "spaghetti2dThickness" -> {
                return NoiseUtils.sampleNoiseAndMapToRange(this.spaghetti2DThicknessModulator, x * 2, y, z * 2, 0.6, 1.3);
            }
            case "spaghetti3d" -> {
                return this.getSpaghetti3D(x, y, z);
            }
            case "spaghetti3dFirst" -> {
                double d = this.spaghetti3DRarityModulator.getValue(x * 2, y, z * 2);
                double e = CaveScalerMixin_scarpetMixin.invokeGetSpaghettiRarity3D(d);
                return sampleWithRarity(this.spaghetti3DNoiseSource1, x, y, z, e);
            }
            case "spaghetti3dSecond" -> {
                double d = this.spaghetti3DRarityModulator.getValue(x * 2, y, z * 2);
                double e = CaveScalerMixin_scarpetMixin.invokeGetSpaghettiRarity3D(d);
                return sampleWithRarity(this.spaghetti3DNoiseSource2, x, y, z, e);
            }
            case "spaghetti3dRarity" -> {
                return this.spaghetti3DRarityModulator.getValue(x * 2, y, z * 2);
            }
            case "spaghetti3dThickness" -> {
                return NoiseUtils.sampleNoiseAndMapToRange(this.spaghetti3DThicknessModulator, x, y, z, 0.065, 0.088);
            }
            case "spaghettiRoughness" -> {
                return this.spaghettiRoughness(x, y, z);
            }
            case "spaghettiRoughnessModulator" -> {
                return NoiseUtils.sampleNoiseAndMapToRange(this.spaghettiRoughnessModulator, x, y, z, 0.0, 0.1);
            }
            case "caveEntrance" -> {
                return this.getBigEntrances(
                        QuartPos.toBlock(x),
                        QuartPos.toBlock(y),
                        QuartPos.toBlock(z)
                );
            }
            case "caveLayer" -> {
                // scaling them by 4 because it's sampled in normal coords
                return this.getLayerizedCaverns(
                        QuartPos.toBlock(x),
                        QuartPos.toBlock(y),
                        QuartPos.toBlock(z)
                );
            }
            case "caveCheese" -> {
                // same reason as above
                return this.cheeseNoiseSource.getValue(
                        QuartPos.toBlock(x),
                        QuartPos.toBlock(y) / 1.5,
                        QuartPos.toBlock(z)
                );
            }
            case "terrainPeaks" -> {
                return terrainNoisePoint.jaggedness();
            }
            case "terrainOffset" -> {
                return terrainNoisePoint.offset();
            }
            case "terrainFactor" -> {
                return terrainNoisePoint.factor();
            }
            case "oreGap" -> {
                return this.gapNoise.getValue(x, y, z);
            }
        }
        throw new InternalExpressionException("Unknown noise type: " + name);
    }
}
