package carpet.script.utils;

import carpet.script.external.Vanilla;
import carpet.script.value.ListValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import carpet.script.value.ValueConversions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public class BiomeInfo
{
    public static final Map<String, BiFunction<ServerLevel, Biome, Value>> biomeFeatures = new HashMap<>()
    {{
        put("tags", (w, b) -> ListValue.wrap(w.registryAccess().registryOrThrow(Registries.BIOME).getTags().filter(p -> p.getSecond().stream().anyMatch(h -> h.value() == b)).map(p -> p.getFirst().location()).map(ValueConversions::of)));
        put("temperature", (w, b) -> NumericValue.of(b.getBaseTemperature()));
        put("fog_color", (w, b) -> ValueConversions.ofRGB(b.getSpecialEffects().getFogColor()));
        put("foliage_color", (w, b) -> ValueConversions.ofRGB(b.getSpecialEffects().getFoliageColorOverride().orElse(4764952))); // client Biome.getDefaultFoliageColor
        put("sky_color", (w, b) -> ValueConversions.ofRGB(b.getSpecialEffects().getSkyColor()));
        put("water_color", (w, b) -> ValueConversions.ofRGB(b.getSpecialEffects().getWaterColor()));
        put("water_fog_color", (w, b) -> ValueConversions.ofRGB(b.getSpecialEffects().getWaterFogColor()));
        put("humidity", (w, b) -> NumericValue.of(Vanilla.Biome_getClimateSettings(b).downfall()));
        put("precipitation", (w, b) -> StringValue.of(b.getPrecipitationAt(new BlockPos(0, w.getSeaLevel(), 0)).name().toLowerCase(Locale.ROOT)));
        put("features", (w, b) -> {
            Registry<ConfiguredFeature<?, ?>> registry = w.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
            return ListValue.wrap(
                    b.getGenerationSettings().features().stream().map(step ->
                            ListValue.wrap(step.stream().map(cfp ->
                                    ValueConversions.of(registry.getKey(cfp.value().feature().value())))
                            )
                    )
            );
        });
    }};
}
