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
import carpet.script.value.EntityValue;
import carpet.script.value.FormattedTextValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Class that holds annotations for Scarpet parameters.
 * @see Param.Strict
 * @see Param.TheLazyT
 * @see Locator.Block
 * @see Locator.Vec3d
 *
 */
public abstract class Param {

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
	 * <p>Determines that this parameter allows passing a value directly instead of a list
	 * of those values.</p>
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
	 * <p>Defines that a parameter of type {@link String}, {@link Text} or {@link ServerPlayerEntity} <b>must</b> be
	 * of its corresponding {@link Value} in order to be accepted (respectively {@link StringValue}, {@link FormattedTextValue}
	 * or {@link EntityValue}).</p>
	 * 
	 * <p>If this annotation is not specified, Carpet will accept any other {@link Value} and call their respective
	 * {@link Value#getString()}, {@code new LiteralText(Value#getString())} or 
	 * {@link EntityValue#getPlayerByValue(net.minecraft.server.MinecraftServer, Value)}.</p>
	 * 
	 * <p>You can define "shallow strictness" ({@link #shallow()}) if you want to allow passing either a {@link StringValue} or 
	 * a {@link FormattedTextValue} to a {@link Text} parameter, but not any {@link Value}.</p>
	 *
	 */
	@Documented
	@Retention(RUNTIME)
	@Target({ PARAMETER, TYPE_USE })
	public @interface Strict {
		/**
		 * <p>Defines whether this parameter can accept types with "shallow strictness", that is, in order to get a {@link Text}, 
		 * accepting either a {@link StringValue} or a {@link FormattedTextValue} as the parameter.</p>
		 * 
		 * <p>Without shallow mode, it would only accept from specifically a {@link FormattedTextValue}.
		 * 
		 * <p>Using this in an unsupported type will throw {@link IllegalArgumentException}, just as if you used the
		 * annotation in an unsupported type.</p>
		 * 
		 * <p>This is {@code false} by default.</p>
		 */
		boolean shallow() default false;
		
		
		static class StrictParams {
			
			
			private StrictParams() {}
		}
	}
	
	public static class Params {
		/**
		 * {@code <Pair<Type, shallow?>, Converter>}
		 */
		private static Map<Pair<Class<?>, Boolean>, ValueConverter<?>> strictParamsByClassAndShallowness = new HashMap<>();
		static {
			strictParamsByClassAndShallowness.put(Pair.of(String.class, false), new SimpleTypeConverter<>(StringValue.class, StringValue::getString));
			strictParamsByClassAndShallowness.put(Pair.of(Text.class, false), new SimpleTypeConverter<>(FormattedTextValue.class, FormattedTextValue::getText));
			strictParamsByClassAndShallowness.put(Pair.of(Text.class, true), new SimpleTypeConverter<>(StringValue.class, 
								v -> v instanceof FormattedTextValue ? ((FormattedTextValue) v).getText() : new LiteralText(v.getString())));
			strictParamsByClassAndShallowness.put(Pair.of(ServerPlayerEntity.class, false), new SimpleTypeConverter<>(EntityValue.class, 
								v -> EntityValue.getPlayerByValue(CarpetServer.minecraft_server, v)));
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
	}
}
