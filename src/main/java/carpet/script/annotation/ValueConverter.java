package carpet.script.annotation;

import org.jetbrains.annotations.Nullable;

import carpet.script.value.Value;

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
}
