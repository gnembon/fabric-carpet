package carpet.script.utils;

import carpet.script.value.BlockValue;
import carpet.script.value.ListValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import carpet.script.value.ValueConversions;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.ConfiguredStructureFeature;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class BiomeInfo
{
    public final static Map<String, BiFunction<ServerWorld, Biome, Value>> biomeFeatures = new HashMap<String, BiFunction<ServerWorld, Biome, Value>>(){{
        put("top_material", (w, b) -> new BlockValue( b.getGenerationSettings().getSurfaceConfig().getTopMaterial(), null, null));
        put("under_material", (w, b) -> new BlockValue( b.getGenerationSettings().getSurfaceConfig().getUnderMaterial(), null, null));
        put("category", (w, b) -> StringValue.of(b.getCategory().getName()));
        put("temperature", (w, b) -> NumericValue.of(b.getTemperature()));
        put("fog_color", (w, b) -> ValueConversions.ofRGB(b.getFogColor()));
        put("foliage_color", (w, b) -> ValueConversions.ofRGB(b.getFoliageColor()));
        put("sky_color", (w, b) -> ValueConversions.ofRGB(b.getSkyColor()));
        put("water_color", (w, b) -> ValueConversions.ofRGB(b.getWaterColor()));
        put("water_fog_color", (w, b) -> ValueConversions.ofRGB(b.getWaterFogColor()));
        put("humidity", (w, b) -> NumericValue.of(b.getDownfall()));
        put("precipitation", (w, b) -> StringValue.of(b.getPrecipitation().getName()));
        put("depth", (w, b) -> NumericValue.of(b.getDepth()));
        put("scale", (w, b) -> NumericValue.of(b.getScale()));
        put("features", (w, b) -> {

            Registry<ConfiguredFeature<?,?>> registry = w.getRegistryManager().get(Registry.CONFIGURED_FEATURE_KEY);
            return ListValue.wrap(
                    b.getGenerationSettings().getFeatures().stream().map(step ->
                            ListValue.wrap(step.stream().map(cfp ->
                                    ValueConversions.of(registry.getId(cfp.get()))
                            ))
                    )
            );
        });
        put("structures", (w, b) -> {
            Registry<ConfiguredStructureFeature<?,?>> registry = w.getRegistryManager().get(Registry.CONFIGURED_STRUCTURE_FEATURE_KEY);
            return ListValue.wrap(b.getGenerationSettings().getStructureFeatures().stream().map(str -> ValueConversions.of(registry.getId(str.get()))));
        });
    }};
}
