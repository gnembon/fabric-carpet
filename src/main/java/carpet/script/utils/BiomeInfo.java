package carpet.script.utils;

import carpet.fakes.PlacedFeatureInterface;
import carpet.script.value.BlockValue;
import carpet.script.value.ListValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import carpet.script.value.ValueConversions;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public class BiomeInfo
{
    public final static Map<String, BiFunction<ServerLevel, Biome, Value>> biomeFeatures = new HashMap<String, BiFunction<ServerLevel, Biome, Value>>(){{
        //put("top_material", (w, b) -> new BlockValue( b.getGenerationSettings(). getSurfaceConfig().getTopMaterial(), null, null));
        //put("under_material", (w, b) -> new BlockValue( b.getGenerationSettings().getSurfaceConfig().getUnderMaterial(), null, null));
        //put("category", (w, b) -> StringValue.of(Biome.getBiomeCategory(Holder.direct(b)).getName()));
        put("tags", (w, b) -> ListValue.wrap(w.registryAccess().registryOrThrow(Registries.BIOME).getTags().filter(p -> p.getSecond().stream().anyMatch(h -> h.value() == b)).map(p -> p.getFirst().location()).map(ValueConversions::of).collect(Collectors.toList())));

        put("temperature", (w, b) -> NumericValue.of(b.getBaseTemperature()));
        put("fog_color", (w, b) -> ValueConversions.ofRGB(b.getSpecialEffects().getFogColor()));
        put("foliage_color", (w, b) -> ValueConversions.ofRGB(b.getSpecialEffects().getFoliageColorOverride().orElse(4764952))); // client Biome.getDefaultFoliageColor
        put("sky_color", (w, b) -> ValueConversions.ofRGB(b.getSpecialEffects().getSkyColor()));
        put("water_color", (w, b) -> ValueConversions.ofRGB(b.getSpecialEffects().getWaterColor()));
        put("water_fog_color", (w, b) -> ValueConversions.ofRGB(b.getSpecialEffects().getWaterFogColor()));
        put("humidity", (w, b) -> NumericValue.of(b.getDownfall()));
        put("precipitation", (w, b) -> StringValue.of(b.getPrecipitation().getName()));
        //put("depth", (w, b) -> NumericValue.of(b.getDepth()));
        //put("scale", (w, b) -> NumericValue.of(b.getScale()));
        put("features", (w, b) -> {

            Registry<ConfiguredFeature<?,?>> registry = w.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
            return ListValue.wrap(
                    b.getGenerationSettings().features().stream().map(step ->
                            ListValue.wrap(step.stream().map(cfp ->
                                    ValueConversions.of(registry.getKey(cfp.value().feature().value())))
                            )
                    )
            );
        });
        //put("structures", (w, b) -> {
        //    Registry<ConfiguredStructureFeature<?,?>> registry = w.getRegistryManager().get(Registry.CONFIGURED_STRUCTURE_FEATURE_KEY);
        //    return ListValue.wrap(b.getGenerationSettings().getStructureFeatures().stream().map(str -> ValueConversions.of(registry.getId(str.get()))));
        //});
    }};
}
