package carpet.script.annotation;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.ParameterizedType;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import carpet.script.Context;
import carpet.script.LazyValue;
import carpet.script.value.Value;

/**
 * <p>Classes implementing this interface are able to convert {@link LazyValue} and {@link Value} instances into
 * {@code <R>}, in order to easily use them in parameters for Scarpet functions created using the {@link LazyFunction}
 * annotation.</p>
 *
 * @param <R> The result type passed {@link LazyValue} or {@link Value}s will be converted to
 */
public interface ValueConverter<R> {
	
	/**
	 * @return The user-friendly name of the result this {@link ValueConverter} converts to, without {@code a} or {@code an},
	 *         and without capitalizing the first letter.
	 * @see #getPrefixedTypeName()
	 */
	public String getTypeName();
	
	/**
	 * Returns the user-friendly name of the result that this {@link ValueConverter} converts to, prefixed with {@code a} or {@code an},
	 * depending on the rules of English (aka starts with {@code aeiou}: an)
	 * 
	 * @implNote This method's default implementation returns the result of {@link #getTypeName()} prefixed depending on whether the first character
	 *           is one of {@code aeiou} or something else.
	 * @see #getTypeName() 
	 */ //TODO Decide whether to keep
	default public String getPrefixedTypeName() {
		switch (getTypeName().charAt(0)) {
			case 'a': case 'e': case 'i': case 'o': case 'u':
				return "an " + getTypeName();
			default:
				return "a " + getTypeName();
		}
	}

	/**
	 * Converts the given {@link Value} to {@code <R>}, which was defined when being registered.
	 * 
	 * <p> Returns {@code null} if one of the conversions failed, either because the {@link Value} was
	 * incompatible in some position of the chain, or because the actual converting function returned {@code null}
	 * (which usually only occurs when the {@link Value} is incompatible/does not hold the appropriate information)
	 * 
	 * <p>Functions using the converter can use {@link #getTypeName()} to get the name of the type this was trying to convert to, 
	 * in case they are not trying to convert to anything else, where it would be recommended to tell the user the name of
	 * the final type instead.
	 * @param value The {@link Value} to convert
	 * @return The converted value, or {@code null} if the conversion failed in the process
	 * @apiNote <p>While most implementations of this method should and will return the type from this method, 
	 *           implementations that <b>require</b> parameters from {@link #evalAndConvert(Iterator, Context)} may
	 *           decide to throw {@link UnsupportedOperationException} in this method and override {@link #evalAndConvert(Iterator, Context)}
	 *           instead. Those implementations, however, should not be available for map or list types, since those can only operate 
	 *           with {@link Value}.</p>
	 *           <p>Currently, the only implementation that requires that is {@link #LAZY_VALUE_IDENTITY}</p>
	 */
	@Nullable
	public R convert(Value value);
	
	/**
	 * <p>Returns whether this {@link ValueConverter} consumes a variable number of elements from the {@link Iterator}
	 * passed to it via {@link #evalAndConvert(Iterator, Context)}.</p>
	 * @implNote The default implementation returns {@code false} by default
	 * @see #howManyValuesDoesThisEat()
	 */
	default public boolean consumesVariableArgs() {
		return false;
	}
	
	/**
	 * <p>Declares the number of {@link LazyValue}s this method consumes from the {@link Iterator} passed to it in
	 * {@link #evalAndConvert(Iterator, Context)}.
	 * <p>Returns {@code -1} if this {@link ValueConverter} can accepts a variable number of arguments <b>and</b>
	 * {@link #consumesVariableArgs()} is {@code true}</p>
	 * @implNote The default implementation returns {@code 1} if {@link #consumesVariableArgs()} is {@code false},
	 *           or {@code -1} if it's {@code true}
	 * TODO Better name
	 */
	default public int howManyValuesDoesThisEat() {
		return consumesVariableArgs() ? -1 : 1;
	}
	
	/**
	 * <p>Gets the proper {@link ValueConverter} for the given {@link AnnotatedType}, considering the type of {@code R[]} as {@code R}.</p>
	 * 
	 * <p>This function does not only consider the actual type (class) of the passed {@link AnnotatedType}, but also its annotations and
	 * generic parameters in order to get the most specific {@link ValueConverter}.</p>
	 * 
	 * <p>Some processing is delegated to the appropriate implementations of {@link ValueConverter} in order to get registered converters 
	 * or generate specific ones for some functions.</p>
	 * 
	 * @param <R> The type of the class the returned {@link ValueConverter} will convert to.
	 *            It is declared from the type in the {@link AnnotatedType} directly inside the function.
	 * @param annoType The {@link AnnotatedType} to search a {@link ValueConverter}.
	 * @return A usable {@link ValueConverter} to convert from a {@link LazyValue} or {@link Value} to {@code <R>}
	 */
	public static <R> ValueConverter<R> fromAnnotatedType(AnnotatedType annoType) {
		Class<R> type = annoType.getType() instanceof ParameterizedType ?
				(Class<R>) ((ParameterizedType)annoType.getType()).getRawType() :
				(Class<R>) annoType.getType(); // We are defining R here
		if (type.isArray()) type = (Class<R>) type.getComponentType(); // Varargs
		
		if (type == List.class)
			return (ValueConverter<R>) ListConverter.fromAnnotatedType(annoType); //Already checked that type is List
		if (type == Map.class)
			return (ValueConverter<R>) MapConverter.fromAnnotatedType(annoType);  //Already checked that type is Map
		if (annoType.getAnnotations().length != 0) { //TODO OptionalParam annotation. Maybe save type to var then wrap into holder?
			if (annoType.getAnnotation(Param.Strict.class) != null)
				return (ValueConverter<R>)Param.Params.getStrictParam(annoType); // Already throws if incorrect usage
		}
		
		//Start: Old fromType. TODO: Move before annotations when OptionalParam exists
		if (type.isAssignableFrom(Value.class))
			return Objects.requireNonNull(ValueCaster.get(type), "Value subclass " + type + " is not registered. Register it in ValueCaster to use it");
		if (type == LazyValue.class)
			return (ValueConverter<R>) LAZY_VALUE_IDENTITY;
		if (type == Context.class)
			return (ValueConverter<R>) CONTEXT_PROVIDER;
		return Objects.requireNonNull(SimpleTypeConverter.get(type), "Type " + type + " is not registered. Register it in SimpleTypeConverter to use it");
	}
	
	/**
	 * <p>Evaluates the next {@link LazyValue} in the given {@link Iterator} with the given {@link Context} and then converts the type
	 * using this {@link ValueConverter}'s {@link #convert(Value)} function.</p>
	 * 
	 * <p>This should be the preferred way to call the converter, since it allows some conversions that
	 * may not be supported by using directly a {@link Value}, allows multi-param converters and allows
	 * meta converters (such as the {@link Context} provider)</p>
	 * 
	 * @implSpec Implementations of this method are not required to evaluate the next {@link LazyValue} if they are not supposed to,
	 *           such as in the case of a {@link LazyValue} to {@link LazyValue} identity, neither move the {@link Iterator} to
	 *           the next position, such as in the case of meta providers like {@link Context}
	 * @param lazyValueIterator An {@link Iterator} holding the {@link LazyValue} to convert in next position
	 * @param context The {@link Context} to convert with
	 * @return The given {@link LazyValue}, evaluated with the given {@link Context}, and converted to the type {@code <R>} of
	 *         this {@link ValueConverter}
	 */
	default public R evalAndConvert(Iterator<LazyValue> lazyValueIterator, Context context) {
		return convert(lazyValueIterator.next().evalValue(context));
	}
	
	/**
	 * A {@link ValueConverter} that outputs the given {@link LazyValue} when running {@link #evalAndConvert(Iterator, Context)},
	 * and throws {@link UnsupportedOperationException} when trying to convert a {@link Value} directly.
	 */
	static final ValueConverter<LazyValue> LAZY_VALUE_IDENTITY = new ValueConverter<LazyValue>() {
		@Override
		public LazyValue convert(Value val) {
			throw new UnsupportedOperationException("Called convert() with a Value in LazyValue identity converter, where only evalAndConvert is supported");
		}
		@Override
		public LazyValue evalAndConvert(Iterator<LazyValue> lazyValueIterator, Context c) {
			return lazyValueIterator.next();
		}
		@Override
		public String getTypeName() {
			return "something"; //TODO Decide between "something" or "value" and use it in ValueCaster too
		}
	};
	
	/**
	 * A {@link ValueConverter} that outputs the {@link Context} in which the function has been called when running
	 * {@link #evalAndConvert(Iterator, Context)}, and throws {@link UnsupportedOperationException} when trying to
	 * convert a {@link Value} directly.
	 */
	static final ValueConverter<Context> CONTEXT_PROVIDER = new ValueConverter<Context>() {
		@Override public String getTypeName() {return null;}

		@Override
		public @Nullable Context convert(Value value) {
			throw new UnsupportedOperationException("Called convert() with a Value in Context Provider converter, where only evalAndConvert is supported");
		}
		
		@Override
		public Context evalAndConvert(Iterator<LazyValue> lazyValueIterator, Context context) {
			return context;
		}
		
		@Override
		public int howManyValuesDoesThisEat() {
			return 0;
		}
	};
}
