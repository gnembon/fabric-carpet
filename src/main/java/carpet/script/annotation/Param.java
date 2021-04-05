package carpet.script.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedType;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;

import carpet.CarpetServer;
import carpet.script.Context;
import carpet.script.LazyValue;
import carpet.script.value.BooleanValue;
import carpet.script.value.EntityValue;
import carpet.script.value.FormattedTextValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Class that holds annotations for Scarpet parameters.
 * @see Param.Strict
 * @see Param.AllowSingleton
 * @see Param.TheLazyT
 * @see Locator.Block
 * @see Locator.Vec3d
 *
 */
public interface Param {

	/**
	 * <p>Indicates that this integer is The Lazy `t`, whatever that actually is.</p>
	 * 
	 * <p>This has no parameters, since The Lazy `t` is The Lazy `t`, without
	 * further discussion</p>
	 *
	 * <p>{@code expression.addLazyFunction("name", -1, (context, t <-- HERE, lv)}</p>
	 */
	@Documented
	@Target(PARAMETER)
	@Retention(RUNTIME)
	public @interface TheLazyT {

	}
	
	/**
	 * <p>Determines that this parameter accepts being passing a value directly instead of a list of those values.</p>
	 * 
	 * <p>Can only be used in {@link List} parameters.</p>
	 *
	 * <p>The function method will receive a singleton of the item in question if there's a single value.</p>
	 */
	@Documented
	@Retention(RUNTIME)
	@Target({PARAMETER, TYPE_USE})
	public @interface AllowSingleton {
	}
	
	/**
	 * <p>Determines that this (and optionally the following parameters) accept either a map of the specified key-value pairs,
	 * a list type [key, value, key2, value2,...] or the same as the list but directly in the parameters (can be disabled in {@link #allowMultiparam()}).</p>
	 * 
	 * <p>Can only be used in {@link Map} types, and {@link #allowMultiparam()} requires it to not be in a type parameter
	 * (since lists and maps contains groups of single items)</p>
	 *
	 * <p><b>IMPORTANT:</b> Using this annotation with {@link #allowMultiparam()} will make this element consume each and every remaining 
	 * value in the function call, therefore it will cause any other parameters (that are not varargs) to throw as if they were not present,
	 * unless they are optional (defined by using Java's {@link Optional} type). They could only be accessed if the parameter at this 
	 * location is specifically a list or map.<br>
	 * Having it as {@code true} will also cause the function to be considered of variable arguments even if it doesn't have varargs.</p>
	 */
	@Documented
	@Retention(RUNTIME)
	@Target({PARAMETER, TYPE_USE})
	public @interface KeyValuePairs {
		/**
		 * <p>Whether or not this accepts the key-value pairs directly in the function call as myFunction(..., key, value, key2, value2)</p>
		 * 
		 * <p>Having this set to {@code true} (as it is by default) has the side effects of effectively converting the method in a variable
		 * parameter count method, and consuming everything remaining in the parameter list unless it finds as first parameter a map or list
		 * to generate the map from, causing any following parameters (except varargs) to throw as if they were not present, unless they are optional.</p>
		 */
		boolean allowMultiparam() default true;
	}
	
	/**
	 * <p>Defines that a parameter of type {@link String}, {@link Text}, {@link ServerPlayerEntity} or {@link Boolean} <b>must</b> be
	 * of its corresponding {@link Value} in order to be accepted (respectively {@link StringValue}, {@link FormattedTextValue},
	 * {@link EntityValue} or {@link BooleanValue}).</p>
	 * 
	 * <p>If this annotation is not specified, Carpet will accept any other {@link Value} and call their respective
	 * {@link Value#getString()}, {@code new LiteralText(Value#getString())}, {@link EntityValue#getPlayerByValue(MinecraftServer, Value)}
	 * or {@link Value#getBoolean()}.</p>
	 * 
	 * <p>You can define "shallow strictness" ({@link #shallow()}) if you want to allow passing both a {@link StringValue} or 
	 * a {@link FormattedTextValue} to a {@link Text} parameter or a {@link NumericValue} to a {@link BooleanValue}, but not any {@link Value}.</p>
	 *
	 */
	@Documented
	@Retention(RUNTIME)
	@Target({ PARAMETER, TYPE_USE })
	public @interface Strict {
		/**
		 * <p>Defines whether this parameter can accept types with "shallow strictness", that is, in order to get a {@link Text}, 
		 * accepting either a {@link StringValue} or a {@link FormattedTextValue} as the parameter, or in order to get a {@link Boolean},
		 * accepting either a {@link NumericValue} or a {@link BooleanValue}.</p>
		 * 
		 * <p>Without shallow mode, it would only accept from specifically a {@link FormattedTextValue} or {@link BooleanValue} respectively.
		 * 
		 * <p>Using this in an unsupported type will throw {@link IllegalArgumentException}, just as if you used the
		 * annotation in an unsupported type.</p>
		 * 
		 * <p>This is {@code false} by default.</p>
		 */
		boolean shallow() default false;
	}
	
	/**
	 * <p>Class that holds the actual converters and converter getting logic for those annotated types and things.</p>
	 * 
	 * <p>It also holds the registry for strict {@link ValueConverter}s.</p>
	 * 
	 * @see #registerStrictConverter(Class, boolean, ValueConverter)
	 *
	 */
	public static class Params {
		/**
		 * <p>A {@link ValueConverter} that outputs the given {@link LazyValue} when running {@link #evalAndConvert(Iterator, Context)},
		 * and throws {@link UnsupportedOperationException} when trying to convert a {@link Value} directly.</p>
		 * 
		 * <p>Public in order to allow custom {@link ValueConverter} to check whether values should be evaluated while testing conditions.</p>
		 */
		public static final ValueConverter<LazyValue> LAZY_VALUE_IDENTITY = new ValueConverter<LazyValue>() {
			@Override
			public LazyValue convert(Value val) {
				throw new UnsupportedOperationException("Called convert() with a Value in LazyValue identity converter, where only evalAndConvert is supported");
			}
			@Override
			public LazyValue evalAndConvert(Iterator<LazyValue> lazyValueIterator, Context c) {
				return lazyValueIterator.hasNext() ? lazyValueIterator.next() : null;
			}
			@Override
			public String getTypeName() {
				return "something"; //TODO Decide between "something" or "value" and use it in ValueCaster too. If first, change prefixedTypeName
			}
		};
		
		/**
		 * <p>A {@link ValueConverter} that outputs the {@link Context} in which the function has been called when running
		 * {@link #evalAndConvert(Iterator, Context)}, and throws {@link UnsupportedOperationException} when trying to
		 * convert a {@link Value} directly.</p>
		 */
		static final ValueConverter<Context> CONTEXT_PROVIDER = new ValueConverter<Context>() {
			@Override public String getTypeName() {return null;}
			@Override
			public Context convert(Value value) {
				throw new UnsupportedOperationException("Called convert() with a Value in Context Provider converter, where only evalAndConvert is supported");
			}
			@Override
			public Context evalAndConvert(Iterator<LazyValue> lazyValueIterator, Context context) {
				return context;
			}
			@Override
			public int valueConsumption() {
				return 0;
			}
		};
		
		/**
		 * <p>Strict converters</p>
		 * <p>Stored as {@code <Pair<Type, shallow?>, Converter>}</p>
		 */
		private static Map<Pair<Class<?>, Boolean>, ValueConverter<?>> strictParamsByClassAndShallowness = new HashMap<>();
		static { //TODO Specify strictness in name?
			registerStrictConverter(String.class, false, new SimpleTypeConverter<>(StringValue.class, StringValue::getString, "string"));
			registerStrictConverter(Text.class, false, new SimpleTypeConverter<>(FormattedTextValue.class, FormattedTextValue::getText, "text"));
			registerStrictConverter(Text.class, true, new SimpleTypeConverter<>(StringValue.class, 
								v -> v instanceof FormattedTextValue ? ((FormattedTextValue) v).getText() : new LiteralText(v.getString()), "text"));
			registerStrictConverter(ServerPlayerEntity.class, false, new SimpleTypeConverter<>(EntityValue.class, 
								v -> EntityValue.getPlayerByValue(CarpetServer.minecraft_server, v), "online player entity"));
			registerStrictConverter(Boolean.TYPE, false, new SimpleTypeConverter<>(BooleanValue.class, BooleanValue::getBoolean, "boolean"));
			registerStrictConverter(Boolean.class, false, new SimpleTypeConverter<>(BooleanValue.class, BooleanValue::getBoolean, "boolean"));
			registerStrictConverter(Boolean.TYPE, true, new SimpleTypeConverter<>(NumericValue.class, NumericValue::getBoolean, "boolean"));
			registerStrictConverter(Boolean.class, true, new SimpleTypeConverter<>(NumericValue.class, NumericValue::getBoolean, "boolean"));
		}
		
		/**
		 * Ya' know, gets the {@link ValueConverter} given the {@link Strict} annotation.
		 * @param type The {@link AnnotatedType} to search the annotation data and class in
		 * @return The {@link ValueConverter} for the specified type and annotation data
		 * @throws IllegalArgumentException If the type doesn't accept the {@link Strict} annotation
		 *                                  or if it has been used incorrectly (shallow in unsupported places)
		 */
		static ValueConverter<?> getStrictConverter(AnnotatedType type) {
			boolean shallow = type.getAnnotation(Strict.class).shallow();
			Class<?> clazz = (Class<?>) type.getType();
			Pair<Class<?>, Boolean> key = Pair.of(clazz, shallow);
			ValueConverter<?> converter = strictParamsByClassAndShallowness.get(key);
			if (converter != null)
				return converter;
			throw new IllegalArgumentException("Incorrect use of @Param.Strict annotation");
		}
		
		/**
		 * <p>Registers a new {@link Param.Strict} parameter converter with the specified shallowness.</p>
		 * 
		 * <p>Registered types should follow the general contract of the rest of {@link Param.Strict} parameter
		 * converters, that is, don't have a shallow-strict converter registered without having a fully-strict
		 * converter available. In order to register completely non-strict converters, those should be registered
		 * in their respective {@link ValueConverter} classes, usually in {@link SimpleTypeConverter}.</p>
		 * 
		 * @param <T> The type class of the return type of the given converter and therefore the generic of itself  
		 * @param type The class instance of the conversion result.
		 * @param shallow {@code true} if you are registering a shallow-strict parameter, {@code false} if a "fully" strict one
		 * @param converter The {@link ValueConverter} for the given type and shallowness.
		 */
		public static <T> void registerStrictConverter(Class<T> type, boolean shallow, ValueConverter<T> converter) {
			Pair<Class<?>, Boolean> key = Pair.of(type, shallow);
			if (strictParamsByClassAndShallowness.containsKey(key))
				throw new IllegalArgumentException(type + " already has a registered " + (shallow ? "" : "non-") + "shallow StrictConverter");
			strictParamsByClassAndShallowness.put(key, converter);
		}
	}
}
