package carpet.script.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import carpet.script.Context;
import carpet.script.value.BooleanValue;
import carpet.script.value.EntityValue;
import carpet.script.value.FormattedTextValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>Class that holds annotations for Scarpet parameters.</p>
 * 
 * @see Param.Strict
 * @see Param.AllowSingleton
 * @see Param.Custom
 * @see Locator.Block
 * @see Locator.Vec3d
 * @see Locator.Function
 *
 */
public interface Param
{
    /**
     * <p>Determines that this parameter accepts being passing a value directly instead of a list of those values.</p>
     * 
     * <p>Can only be used in {@link List} parameters.</p>
     *
     * <p>The function method will receive a singleton of the item in question if there's a single value.</p>
     */
    @Documented
    @Retention(RUNTIME)
    @Target({ PARAMETER, TYPE_USE })
    @interface AllowSingleton
    {

    }

    /**
     * <p>Determines that this (and optionally the following parameters) accept either a map of the specified key-value pairs, a list type [key,
     * value, key2, value2,...] or the same as the list but directly in the parameters (can be disabled in {@link #allowMultiparam()}).</p>
     * 
     * <p>Can only be used in {@link Map} types, and {@link #allowMultiparam()} requires it to not be in a type parameter (since lists and maps
     * contains groups of single items)</p>
     *
     * <p><b>IMPORTANT:</b> Using this annotation with {@link #allowMultiparam()} will make this element consume each and every remaining value in the
     * function call, therefore it will cause any other parameters (that are not varargs) to throw as if they were not present, unless they are
     * optional (defined by using Java's {@link Optional} type). They could only be accessed if the parameter at this location is specifically a list
     * or map.<br> Having it as {@code true} will also cause the function to be considered of variable arguments even if it doesn't have varargs.</p>
     */
    @Documented
    @Retention(RUNTIME)
    @Target({ PARAMETER, TYPE_USE })
    @interface KeyValuePairs
    {
        /**
         * <p>Whether or not this accepts the key-value pairs directly in the function call as myFunction(..., key, value, key2, value2)</p>
         * 
         * <p>Having this set to {@code true} (as it is by default) has the side effects of effectively converting the method in a variable parameter
         * count method, and consuming everything remaining in the parameter list unless it finds as first parameter a map or list to generate the map
         * from, causing any following parameters (except varargs) to throw as if they were not present, unless they are optional.</p>
         */
        boolean allowMultiparam() default true;
    }

    /**
     * <p>Defines that the parameter's converter has to be retrieved from the custom converter storage, in order to allow extensions to register
     * <b>complex</b> {@link ValueConverter}s. You can register such converters in {@link Params#registerCustomConverterFactory(BiFunction)}, although
     * if you only need a simple {@link Value}->something converter you should be looking at {@link SimpleTypeConverter} instead</p>
     */
    @Documented
    @Retention(RUNTIME)
    @Target({ PARAMETER, TYPE_USE })
    @interface Custom
    {

    }

    /**
     * <p>Defines that a parameter of type {@link String}, {@link Component}, {@link ServerPlayer}, {@link Boolean} or other registered strict type
     * <b>must</b> be of its corresponding {@link Value} in order to be accepted (respectively {@link StringValue}, {@link FormattedTextValue},
     * {@link EntityValue} or {@link BooleanValue}).</p>
     * 
     * <p>If this annotation is not specified, Carpet will accept any other {@link Value} and call respectively {@link Value#getString()},
     * {@code new LiteralText(Value#getString())}, {@link EntityValue#getPlayerByValue(MinecraftServer, Value)} or {@link Value#getBoolean()}.</p>
     * 
     * <p>You can define "shallow strictness" ({@link #shallow()}) if you want to allow passing both a {@link StringValue} or a
     * {@link FormattedTextValue} to a {@link Component} parameter or a {@link NumericValue} to a {@link BooleanValue}, but not any {@link Value}.</p>
     *
     */
    @Documented
    @Retention(RUNTIME)
    @Target({ PARAMETER, TYPE_USE })
    @interface Strict
    {
        /**
         * <p>Defines whether this parameter can accept types with "shallow strictness", that is, in order to get a {@link Component}, accepting either a
         * {@link StringValue} or a {@link FormattedTextValue} as the parameter, or in order to get a {@link Boolean}, accepting either a
         * {@link NumericValue} or a {@link BooleanValue}.</p>
         * 
         * <p>Without shallow mode, it would only accept from specifically a {@link FormattedTextValue} or {@link BooleanValue} respectively.
         * 
         * <p>Using this in an unsupported type will throw {@link IllegalArgumentException}, just as if you used the annotation in an unsupported
         * type.</p>
         * 
         * <p>This is {@code false} by default.</p>
         */
        boolean shallow() default false;
    }

    /**
     * <p>Class that holds the actual converters and converter getting logic for those annotated types and things.</p>
     * 
     * <p>It also holds the registry for strict and custom {@link ValueConverter}s.</p>
     * 
     * @see #registerStrictConverter(Class, boolean, ValueConverter)
     * @see #registerCustomConverterFactory(BiFunction)
     *
     */
    final class Params
    {
        /**
         * <p>A {@link ValueConverter} that outputs the {@link Context} in which the function has been called, and throws {@link UnsupportedOperationException} when trying to convert a {@link Value}
         * directly.</p>
         */
        static final ValueConverter<Context> CONTEXT_PROVIDER = new ValueConverter<>()
        {
            @Nullable
            @Override
            public String getTypeName()
            {
                return null;
            }

            @Override
            public Context convert(Value value, @Nullable Context context)
            {
                throw new UnsupportedOperationException("Called convert() with Value in Context Provider converter, where only checkAndConvert is supported");
            }

            @Override
            public Context checkAndConvert(Iterator<Value> valueIterator, Context context, Context.Type theLazyT)
            {
                return context;
            }

            @Override
            public int valueConsumption()
            {
                return 0;
            }
        };
        
        /**
         * <p>A {@link ValueConverter} that outputs the {@link Context.Type} which the function has been called, or throws {@link UnsupportedOperationException} when trying to convert a {@link Value}
         * directly.</p>
         */
        static final ValueConverter<Context.Type> CONTEXT_TYPE_PROVIDER = new ValueConverter<>()
        {
            @Nullable
            @Override
            public String getTypeName()
            {
                return null;
            }

            @Override
            public Context.Type convert(Value value, @Nullable Context context)
            {
                throw new UnsupportedOperationException("Called convert() with a Value in TheLazyT Provider, where only checkAndConvert is supported");
            }

            @Override
            public Context.Type checkAndConvert(Iterator<Value> valueIterator, Context context, Context.Type theLazyT)
            {
                return theLazyT;
            }

            @Override
            public int valueConsumption()
            {
                return 0;
            }
        };

        record StrictConverterInfo(Class<?> type, boolean shallow) {}
        private static final Map<StrictConverterInfo, ValueConverter<?>> strictParamsByClassAndShallowness = new HashMap<>();
        static
        { // TODO Specify strictness in name?
            registerStrictConverter(String.class, false, new SimpleTypeConverter<>(StringValue.class, StringValue::getString, "string"));
            registerStrictConverter(Component.class, false, new SimpleTypeConverter<>(FormattedTextValue.class, FormattedTextValue::getText, "text"));
            registerStrictConverter(Component.class, true, new SimpleTypeConverter<>(StringValue.class, FormattedTextValue::getTextByValue, "text"));
            registerStrictConverter(ServerPlayer.class, false, new SimpleTypeConverter<>(EntityValue.class,
                    v -> EntityValue.getPlayerByValue(v.getEntity().getServer(), v), "online player entity"));
            registerStrictConverter(Boolean.class, false, new SimpleTypeConverter<>(BooleanValue.class, BooleanValue::getBoolean, "boolean"));
            registerStrictConverter(Boolean.class, true, new SimpleTypeConverter<>(NumericValue.class, NumericValue::getBoolean, "boolean"));
        }

        /**
         * Ya' know, gets the {@link ValueConverter} given the {@link Strict} annotation.
         * 
         * @param type The {@link AnnotatedType} to search the annotation data and class in
         * @return The {@link ValueConverter} for the specified type and annotation data
         * @throws IllegalArgumentException If the type doesn't accept the {@link Strict} annotation or if it has been used incorrectly (shallow in
         *                                  unsupported places)
         */
        static ValueConverter<?> getStrictConverter(AnnotatedType type)
        {
            boolean shallow = type.getAnnotation(Strict.class).shallow();
            Class<?> clazz = (Class<?>) type.getType();
            StrictConverterInfo key = new StrictConverterInfo(clazz, shallow);
            ValueConverter<?> converter = strictParamsByClassAndShallowness.get(key);
            if (converter != null)
            {
                return converter;
            }
            throw new IllegalArgumentException("Incorrect use of @Param.Strict annotation");
        }

        /**
         * <p>Registers a new {@link Param.Strict} parameter converter with the specified shallowness.</p>
         * 
         * <p>Registered types should follow the general contract of the rest of {@link Param.Strict} parameter converters, that is, don't have a
         * shallow-strict converter registered without having a fully-strict converter available. In order to register completely non-strict
         * converters, those should be registered in their respective {@link ValueConverter} classes, usually in {@link SimpleTypeConverter}.</p>
         * 
         * @param <T>       The type class of the return type of the given converter and therefore the generic of itself
         * @param type      The class instance of the conversion result.
         * @param shallow   {@code true} if you are registering a shallow-strict parameter, {@code false} if a "fully" strict one
         * @param converter The {@link ValueConverter} for the given type and shallowness.
         */
        public static <T> void registerStrictConverter(Class<T> type, boolean shallow, ValueConverter<T> converter)
        {
            StrictConverterInfo key = new StrictConverterInfo(type, shallow);
            if (strictParamsByClassAndShallowness.containsKey(key))
            {
                throw new IllegalArgumentException(type + " already has a registered " + (shallow ? "" : "non-") + "shallow StrictConverter");
            }
            strictParamsByClassAndShallowness.put(key, converter);
        }

        private static final List<BiFunction<AnnotatedType, Class<?>, ValueConverter<?>>> customFactories = new ArrayList<>();

        /**
         * <p>Allows extensions to register <b>COMPLEX</b> {@link ValueConverter} factories in order to be used with the {@link Param.Custom}
         * annotation.</p> <p><b>If you only need to register a converter from a {@link Value} to a type, use
         * {@link SimpleTypeConverter#registerType(Class, Class, java.util.function.Function, String)} instead. This is intended to be used when you
         * need more granular control over the conversion, such as custom extra parameters via annotations, converters using multiple values, or even
         * a variable number of values.</b></p> <p>The annotation parser will loop through all registered custom converter factories when searching
         * for the appropriate {@link ValueConverter} for a parameter annotated with the {@link Param.Custom} annotation.</p> <p>Factories are
         * expected to return {@code null} when the provided arguments don't match a {@link ValueConverter} they are able to create (or reuse).</p>
         * <p>You have {@link ValueCaster#get(Class)} and {@link ValueConverter#fromAnnotatedType(AnnotatedType)} available in case you need to get
         * valid {@link ValueConverter}s for things such as nested types, intermediary conversions or whatever you really need them for.</p>
         * 
         * @param <T>     The type that the ValueConverter will convert to. Its class will also be passed to the factory
         * @param factory A {@link BiFunction} that provides {@link ValueConverter}s given an {@link AnnotatedType} and the {@link Class} of its type,
         *                for convenience reasons. The factory must return {@code null} if the specific conditions required to return a valid
         *                converter are not met, therefore letting other registered factories try get theirs. Factories must also ensure that the
         *                returned {@link ValueConverter} converts to the given {@link Class}, and that the {@link ValueConverter} follows the
         *                contract of {@link ValueConverter}s, which can be found in its Javadoc. Factories should try to be specific in order to
         *                avoid possible collisions with other extensions.
         */
        @SuppressWarnings("unchecked") // this makes no sense... But I guess its preferable to enforce typesafety in callers
        public static <T> void registerCustomConverterFactory(BiFunction<AnnotatedType, Class<T>, ValueConverter<T>> factory)
        {
            customFactories.add((BiFunction<AnnotatedType, Class<?>, ValueConverter<?>>) (Object) factory);
        }

        @SuppressWarnings("unchecked") // Stored correctly
        static <R> ValueConverter<R> getCustomConverter(AnnotatedType annoType, Class<R> type)
        {
            ValueConverter<R> result;
            for (BiFunction<AnnotatedType, Class<?>, ValueConverter<?>> factory : customFactories)
            {
                if ((result = (ValueConverter<R>) factory.apply(annoType, type)) != null)
                {
                    return result;
                }
            }
            throw new IllegalArgumentException("No custom converter found for Param.Custom annotated param with type " + annoType.getType().getTypeName());
        }
    }
}
