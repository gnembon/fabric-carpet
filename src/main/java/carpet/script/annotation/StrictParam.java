package carpet.script.annotation;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedType;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import carpet.script.value.FormattedTextValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import net.minecraft.text.Text;
import net.minecraft.text.LiteralText;

/**
 * <p>Defines that a parameter of type {@link String} or {@link Text} <b>must</b> be
 * of its corresponding {@link Value} in order to be accepted ({@link StringValue} or {@link FormattedTextValue}.</p>
 * 
 * <p>If this annotation is not specified, Carpet will accept any other {@link Value} and call their respective
 * {@link Value#getString()} or call {@code Text#of(Value#getString())}.</p>
 * 
 * <p>You can define "shallow strictness" if you want to allow passing either a {@link StringValue} or {@link FormattedTextValue}
 * to a {@link Text} parameter, but not any {@link Value}.</p>
 *
 */
@Documented
@Retention(RUNTIME)
@Target({ PARAMETER, TYPE_PARAMETER })
public @interface StrictParam {
	/**
	 * <p>Defines whether this parameter can accept types with "shallow strictness", that is, in order to get a {@link Text}, 
	 * accepting either a {@link StringValue} or a {@link FormattedTextValue} as the parameter.</p>
	 * 
	 * <p>Without shallow mode, it would only accept from specifically a {@link FormattedTextValue}.
	 * 
	 * <p>Using this in an unsupported type will throw {@link UnsupportedOperationException}, just as if you used the
	 * annotation in an unsupported type.</p>
	 * 
	 * <p>This is {@code false} by default.</p>
	 */
	boolean shallow() default false;
	
	
	static class StrictParamHolder {
		/**
		 * {@code <Pair<Type, shallow?>, Converter>}
		 */
		private static Map<Pair<Class<?>, Boolean>, ValueConverter<?>> byClassAndShallowness = new HashMap<>();
		static {
			byClassAndShallowness.put(Pair.of(Text.class, false), new SimpleTypeConverter<>(StringValue.class, StringValue::getString));
			byClassAndShallowness.put(Pair.of(Text.class, false), new SimpleTypeConverter<>(FormattedTextValue.class, FormattedTextValue::getText));
			byClassAndShallowness.put(Pair.of(Text.class, true), new SimpleTypeConverter<>(StringValue.class, 
								v -> v instanceof FormattedTextValue ? ((FormattedTextValue) v).getText() : new LiteralText(v.getString())));
		}//Accommodating for new types. I think I won't allow Value::getDoubleValue as default, so maybe reconsider taking this back to an if-chain?
		
		/**
		 * Ya' know, gets the {@link ValueConverter} given the {@link StrictParam} annotation.
		 * @param type The {@link AnnotatedType} to search the annotation data and class in
		 * @return The {@link ValueConverter} for the specified type and annotation data
		 * @throws IllegalArgumentException If the type doesn't accept the {@link StrictParam} annotation
		 *                                  or if it has been used incorrectly (shallow in unsupported places)
		 */
		public static ValueConverter<?> get(AnnotatedType type) { //TODO Check this new method works
			boolean shallow = type.getAnnotation(StrictParam.class).shallow();
			Class<?> clazz = (Class<?>) type.getType();
			Pair<Class<?>, Boolean> key = Pair.of(clazz, shallow);
			ValueConverter<?> converter = byClassAndShallowness.get(key);
			if (converter != null)
				return converter;
			throw new IllegalArgumentException("Incorrect use of @StrictParam annotation");
		}
		
		private StrictParamHolder(){}
	}
}
