package carpet.script.annotation;

import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import carpet.script.value.Value;

/**
 * <p>Classes implementing this interface are able to convert {@link Value} instances into
 * R, in order to easily use them in parameters for Scarpet functions created using the {@link LazyFunction}
 * annotation.</p>
 *
 * @param <R> The result type passed {@link Value}s will be converted to
 */
public interface ValueConverter<R> {
	
	/**
	 * @return The user-friendly name of the result this {@link ValueConverter} converts to
	 */
	public String getTypeName();

	/**
	 * Converts the given {@link Value} to {@code R}, which was defined when being registered.
	 * 
	 * <p> Returns {@code null} if one of the conversions failed, either because the {@link Value} was
	 * incompatible in some position of the chain, or because the actual converting function returned {@code null}
	 * (which usually only occurs when the {@link Value} is incompatible/does not hold the appropriate information)
	 * 
	 * <p>Functions using the converter can use {@link #getTypeName()} to get the name of the type this was trying to convert to, 
	 * in case they are not trying to convert to anything else, where it would be recommended to tell the user the name of
	 * the final type instead.
	 * @param value The {@link Value} to convert
	 * @return The converted value
	 */
	@Nullable
	public R convert(Value value);
	
	public static <R> ValueConverter<R> fromParam(Parameter param) {
		Class<R> type = (Class<R>) param.getType(); // We are defining R here
		if (type.isAssignableFrom(List.class))
			return (ValueConverter<R>) ListConverter.fromParameter(param); //Already checked that type is List
		if (type.isAssignableFrom(Map.class))
			return null; //TODO Map converter
		if (type.getAnnotations().length != 0)
			return null; //TODO locators and things
		return fromType(type);
	}
	
	public static <R> ValueConverter<R> fromType(Class<R> outputType) {
		if (outputType.isAssignableFrom(Value.class))
			return ValueCaster.getOrRegister(outputType);
		return SimpleTypeConverter.get(outputType);
	}
}
