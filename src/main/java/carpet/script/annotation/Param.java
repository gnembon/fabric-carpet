package carpet.script.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import carpet.CarpetServer;
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
	 * <p>Determines that this parameter allows passing a value directly instead of a list of those values.</p>
	 * 
	 * <p>Can be used both in {@link List} parameters and in the generic types of those (eg {@code List<@AllowSingleton List<Entity>>}</p>
	 *
	 */
	@Documented
	@Retention(RUNTIME)
	@Target({PARAMETER, TYPE_USE})
	public @interface AllowSingleton {
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
	 */
	public static class Params {
		/**
		 * {@code <Pair<Type, shallow?>, Converter>}
		 */
		private static Map<Pair<Class<?>, Boolean>, ValueConverter<?>> strictParamsByClassAndShallowness = new HashMap<>();
		static {
			registerStrictParam(String.class, false, new SimpleTypeConverter<>(StringValue.class, StringValue::getString));
			registerStrictParam(Text.class, false, new SimpleTypeConverter<>(FormattedTextValue.class, FormattedTextValue::getText));
			registerStrictParam(Text.class, true, new SimpleTypeConverter<>(StringValue.class, 
								v -> v instanceof FormattedTextValue ? ((FormattedTextValue) v).getText() : new LiteralText(v.getString())));
			registerStrictParam(ServerPlayerEntity.class, false, new SimpleTypeConverter<>(EntityValue.class, 
								v -> EntityValue.getPlayerByValue(CarpetServer.minecraft_server, v)));
			registerStrictParam(Boolean.TYPE, false, new SimpleTypeConverter<>(BooleanValue.class, BooleanValue::getBoolean));
			registerStrictParam(Boolean.class, false, new SimpleTypeConverter<>(BooleanValue.class, BooleanValue::getBoolean));
			registerStrictParam(Boolean.TYPE, true, new SimpleTypeConverter<>(NumericValue.class, NumericValue::getBoolean));
		}
		
		/**
		 * Ya' know, gets the {@link ValueConverter} given the {@link Strict} annotation.
		 * @param type The {@link AnnotatedType} to search the annotation data and class in
		 * @return The {@link ValueConverter} for the specified type and annotation data
		 * @throws IllegalArgumentException If the type doesn't accept the {@link Strict} annotation
		 *                                  or if it has been used incorrectly (shallow in unsupported places)
		 */
		public static ValueConverter<?> getStrictParam(AnnotatedType type) { //TODO Check this new method works
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
		public static <T> void registerStrictParam(Class<T> type, boolean shallow, ValueConverter<T> converter) {
			Pair<Class<?>, Boolean> key = Pair.of(type, shallow);
			strictParamsByClassAndShallowness.put(key, converter);
		}
	}
}
