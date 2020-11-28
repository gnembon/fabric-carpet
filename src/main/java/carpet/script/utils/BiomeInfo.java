package carpet.script.utils;

import carpet.script.value.BlockValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import carpet.script.value.ValueConversions;
import net.minecraft.world.biome.Biome;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class BiomeInfo
{
    public final static Map<String, Function<Biome, Value>> biomeFeatures = new HashMap<String, Function<Biome, Value>>(){{
        put("top_material", b -> new BlockValue( b.getGenerationSettings().getSurfaceConfig().getTopMaterial(), null, null));
        put("under_material", b -> new BlockValue( b.getGenerationSettings().getSurfaceConfig().getUnderMaterial(), null, null));
        put("category", b -> StringValue.of(b.getCategory().getName()));
        put("temperature", b -> NumericValue.of(b.getTemperature()));
        put("fog_color", b -> ValueConversions.ofRGB(b.getFogColor()));
        put("foliage_color", b -> ValueConversions.ofRGB(b.getFoliageColor()));
        put("sky_color", b -> ValueConversions.ofRGB(b.getSkyColor()));
        put("water_color", b -> ValueConversions.ofRGB(b.getWaterColor()));
        put("water_fog_color", b -> ValueConversions.ofRGB(b.getWaterFogColor()));
        put("humidity", b -> NumericValue.of(b.getDownfall()));
        put("precipitation", b -> StringValue.of(b.getPrecipitation().getName()));
        put("depth", b -> NumericValue.of(b.getDepth()));
        put("scale", b -> NumericValue.of(b.getScale()));
        //put("features", b -> b.getGenerationSettings().getFeatures())
    }};
}
