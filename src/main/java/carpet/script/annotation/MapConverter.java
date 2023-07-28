package carpet.script.annotation;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import carpet.script.Context;
import carpet.script.value.ListValue;
import carpet.script.value.MapValue;
import carpet.script.value.Value;

import javax.annotation.Nullable;

/**
 * <p>Converts a {@link MapValue} to a {@link Map}, converting all of its contents to their respective types.</p>
 * 
 * <p>If the {@link Param.KeyValuePairs} annotation is specified, uses its subclass at {@link PairConverter} and allows passing either a map, a list
 * like [key, value, key2, value2,...] or the same as the list inlined in the function's body, unless {@link Param.KeyValuePairs#allowMultiparam()} is
 * set to {@code false}.</p>
 * 
 * <p>Maps provided by this converter are <b>not</b> linked to the initial map, and therefore will not reflect changes in either of them.</p>
 *
 * @param <K> The type of the map's keys
 * @param <V> The type of the map's values
 */
class MapConverter<K, V> implements ValueConverter<Map<K, V>>
{
    protected final ValueConverter<K> keyConverter;
    protected final ValueConverter<V> valueConverter;

    @Override
    public String getTypeName()
    {
        return "map with " + keyConverter.getTypeName() + "s as the key and " + valueConverter.getTypeName() + "s as the value";
    }

    @Override
    public Map<K, V> convert(Value value, @Nullable Context context)
    {
        Map<K, V> result = new HashMap<>();
        if (value instanceof MapValue)
        {
            for (Entry<Value, Value> entry : ((MapValue) value).getMap().entrySet())
            {
                K key = keyConverter.convert(entry.getKey(), context);
                V val = valueConverter.convert(entry.getValue(), context);
                if (key == null || val == null)
                {
                    return null;
                }
                result.put(key, val);
            }
            return result;
        }
        return null;
    }

    private MapConverter(AnnotatedType keyType, AnnotatedType valueType)
    {
        super();
        keyConverter = ValueConverter.fromAnnotatedType(keyType);
        valueConverter = ValueConverter.fromAnnotatedType(valueType);
    }

    /**
     * <p>Returns a new {@link MapConverter} to convert to the given {@link AnnotatedType}.</p>
     * 
     * <p>The returned {@link ValueConverter} will convert the objects inside the map (keys and values) to the generics specified in the
     * {@link AnnotatedType}.</p>
     * 
     * <p>If the provided {@link AnnotatedType} is annotated with {@link Param.KeyValuePairs}, it will return an implementation of
     * {@link MapConverter} that provides extra checks in order to allow inputs of either {@code {k->v,k2->v2,...}}, {@code [k,v,k2,v2,...]} or
     * {@code fn(...,k,v,k2,v2,...)}</p>
     * 
     * @apiNote This method expects the {@link AnnotatedType} to already be of {@link Map} type, and, while it will technically accept a
     *          non-{@link Map} {@link AnnotatedType}, it will fail if it doesn't has at least two generic parameters with an
     *          {@link ArrayIndexOutOfBoundsException}.
     * @param annotatedType The type to get generics information from
     * @return A new {@link MapConverter} for the data specified in the {@link AnnotatedType}
     */
    static MapConverter<?, ?> fromAnnotatedType(AnnotatedType annotatedType)
    {
        AnnotatedType[] annotatedGenerics = ((AnnotatedParameterizedType) annotatedType).getAnnotatedActualTypeArguments();
        return annotatedType.isAnnotationPresent(Param.KeyValuePairs.class)
                ? new PairConverter<>(annotatedGenerics[0], annotatedGenerics[1], annotatedType.getAnnotation(Param.KeyValuePairs.class))
                : new MapConverter<>(annotatedGenerics[0], annotatedGenerics[1]);
    }

    private static final class PairConverter<K, V> extends MapConverter<K, V>
    {
        private final boolean acceptMultiParam;

        private PairConverter(AnnotatedType keyType, AnnotatedType valueType, Param.KeyValuePairs config)
        {
            super(keyType, valueType);
            acceptMultiParam = config.allowMultiparam();
        }

        @Override
        public boolean consumesVariableArgs()
        {
            return acceptMultiParam;
        }

        @Nullable
        @Override
        public Map<K, V> convert(Value value, @Nullable Context context) {
            return value instanceof MapValue ? super.convert(value, context)
                    : value instanceof ListValue ? convertList(((ListValue)value).getItems(), context)
                            : null; // Multiparam mode can only be used in evalAndConvert 
        }


        @Nullable
        private Map<K, V> convertList(List<Value> valueList, @Nullable Context context)
        {
            if (valueList.size() % 2 == 1)
            {
                return null;
            }
            Map<K, V> map = new HashMap<>();
            Iterator<Value> val = valueList.iterator();
            while (val.hasNext())
            {
                K key = keyConverter.convert(val.next(), context);
                V value = valueConverter.convert(val.next(), context);
                if (key == null || value == null)
                {
                    return null;
                }
                map.put(key, value);
            }
            return map;
        }

        @Nullable
        @Override
        public Map<K, V> checkAndConvert(Iterator<Value> valueIterator, Context context, Context.Type theLazyT)
        {
            if (!valueIterator.hasNext())
            {
                return null;
            }
            Value val = valueIterator.next();
            if (!acceptMultiParam || val instanceof MapValue || (val instanceof ListValue && !(keyConverter instanceof ListConverter)))
            {
                return convert(val, context);                              // @KeyValuePairs Map<List<Something>, Boolean> will not support list consumption
            }
            
            Map<K, V> map = new HashMap<>();
            K key = keyConverter.convert(val, context); //First pair is manual since we got it to check for a different conversion mode
            V value = valueConverter.checkAndConvert(valueIterator, context, theLazyT);
            if (key == null || value == null)
            {
                return null;
            }
            map.put(key, value);
            while (valueIterator.hasNext())
            {
                key = keyConverter.checkAndConvert(valueIterator, context, theLazyT);
                value = valueConverter.checkAndConvert(valueIterator, context, theLazyT);
                if (key == null || value == null)
                {
                    return null;
                }
                map.put(key, value);
            }
            return map;
        }

        @Override
        public String getTypeName()
        {
            return "either a map of key-value pairs" + (acceptMultiParam ? "," : " or") + " a list in the form of [key, value, key2, value2,...]"
                    + (acceptMultiParam ? " or those key-value pairs in the function" : "") + " (keys being " + keyConverter.getTypeName()
                    + "s and values being " + valueConverter.getTypeName() + "s)";
        }
    }
}
