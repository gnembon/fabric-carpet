package carpet.script.annotation;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.util.HashMap;
import java.util.Map;

import carpet.script.value.MapValue;
import carpet.script.value.Value;

public class MapConverter<K, V> implements ValueConverter<Map<K, V>> {
	private final ValueConverter<K> keyConverter;
	private final ValueConverter<V> valueConverter;
	
	@Override
	public String getTypeName() {
		return "map with " + keyConverter.getTypeName() + "s as the key and " + valueConverter.getTypeName() + "s as the value";
	}

	@Override
	public Map<K, V> convert(Value value) {
		Map<K, V> result = new HashMap<>(); //Would love a way to get this directly in a one-line stream. Also TODO check nulls
		if (value instanceof MapValue) {
			((MapValue) value).getMap().forEach((k, v) -> result.put(keyConverter.convert(k), valueConverter.convert(v)));
			return result;
		}
		return null;
	}

	private MapConverter(AnnotatedType keyType, AnnotatedType valueType) {
		keyConverter = ValueConverter.fromAnnotatedType(keyType);
		valueConverter = ValueConverter.fromAnnotatedType(valueType);
	}
	
	public static MapConverter<?, ?> fromAnnotatedType(AnnotatedType annotatedType) { //TODO Assert actual type-safety (or at least kinda)
		AnnotatedType[] annotatedParams = ((AnnotatedParameterizedType) annotatedType).getAnnotatedActualTypeArguments();
		return new MapConverter<>(annotatedParams[0], annotatedParams[1]);
	}
}
