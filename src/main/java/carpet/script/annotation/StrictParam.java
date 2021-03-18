package carpet.script.annotation;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedType;

import carpet.script.value.FormattedTextValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import net.minecraft.text.Text;

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
	 * <p>Defines whether this parameter can accept types with "shallow strictness", that is, accepting either a {@link StringValue} or 
	 * a {@link FormattedTextValue} as the parameter.</p>
	 * 
	 * <p>This is {@code false} by default.</p>
	 */
	boolean shallow() default false;
	
	
	static class StrictParamHolder {
		public static final ValueConverter<String> STRICT_STRING_CONVERTER = new ValueConverter<String>() {
			@Override
			public String getTypeName() {
				return "string";
			}

			@Override
			public String convert(Value value) {
				return value instanceof StringValue ? value.getString() : null;
			}
		};
		
		public static final ValueConverter<Text> STRICT_TEXT_CONVERTER = new ValueConverter<Text>() {
			@Override
			public String getTypeName() {
				return "formatted text";
			}

			@Override
			public Text convert(Value value) {
				return value instanceof FormattedTextValue ? ((FormattedTextValue)value).getText() : null;
			}
		};
		
		public static final ValueConverter<Text> SHALLOW_STRICT_TEXT_CONVERTER = new ValueConverter<Text>() {
			@Override
			public String getTypeName() {
				return "string or formatted text";
			}

			@Override
			public Text convert(Value value) {
				return value instanceof FormattedTextValue ? ((FormattedTextValue)value).getText() 
						: value instanceof StringValue ? Text.of(value.getString()) : null;
			}
		};
		
		/**
		 * Ya' know, gets the {@link ValueConverter} given the {@link StrictParam} annotation.
		 * @param type The {@link AnnotatedType} to search the annotation data and class in
		 * @throws IllegalArgumentException If the annotation wasn't used correctly.
		 * @return
		 */
		public static ValueConverter<?> get(AnnotatedType type) {
			boolean shallow = type.getAnnotation(StrictParam.class).shallow();
			Class<?> clazz = (Class<?>)type.getType();
			if (clazz == String.class && !shallow) {
				return STRICT_STRING_CONVERTER;
			}
			if (clazz == Text.class)
				return shallow ? SHALLOW_STRICT_TEXT_CONVERTER : STRICT_TEXT_CONVERTER;
			throw new IllegalArgumentException("Incorrect use of @StrictParam annotation");
		}
		
		private StrictParamHolder(){}
	}
}
