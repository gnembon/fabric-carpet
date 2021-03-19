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
		Map<K, V> result = new HashMap<>(); //Would love a way to get this directly in a one-line stream. Also TODO check nulls from converters
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
	
	/**
	 * <p>Returns a new {@link MapConverter} to convert to the given {@link AnnotatedType}.</p>
	 * 
	 * <p>The given {@link ValueConverter} will convert the objects inside the map (keys and values) to the
	 * generics specified in the {@link AnnotatedType}.</p>
	 * 
	 * @apiNote This method expects the {@link AnnotatedType} to already be of {@link Map} type, and, while it will
	 *          technically accept a non-{@link Map} {@link AnnotatedType}, it will fail if it doesn't has at least
	 *          two generic parameters with an {@link ArrayIndexOutOfBoundsException}. 
	 * @param annotatedType The type to get generics information from
	 * @return A new {@link ListConverter} for the data specified in the {@link AnnotatedType}
	 */
	public static MapConverter<?, ?> fromAnnotatedType(AnnotatedType annotatedType) { //TODO Assert actual type-safety (or at least kinda)
		AnnotatedType[] annotatedParams = ((AnnotatedParameterizedType) annotatedType).getAnnotatedActualTypeArguments();
		return new MapConverter<>(annotatedParams[0], annotatedParams[1]);
	}
}
