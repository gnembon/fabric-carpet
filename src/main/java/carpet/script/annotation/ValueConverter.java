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
import carpet.script.annotation.Param.Params;
import carpet.script.value.Value;

/**
 * <p>Classes implementing this interface are able to convert {@link LazyValue} and {@link Value} instances into
 * {@code <R>}, in order to easily use them in parameters for Scarpet functions created using the {@link LazyFunction}
 * annotation.</p>
 *
 * @param <R> The result type that the passed {@link LazyValue} or {@link Value}s will be converted to
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
		if (getTypeName().isEmpty()) return "";
		switch (getTypeName().charAt(0)) {
			case 'a': case 'e': case 'i': case 'o': case 'u':
				return "an " + getTypeName();
			default:
				return "a " + getTypeName();
		}
	}

	/**
	 * <p>Converts the given {@link Value} to {@code <R>}, which was defined when being registered.</p>
	 * 
	 * <p> Returns {@code null} if one of the conversions failed, either because the {@link Value} was
	 * incompatible in some position of the chain, or because the actual converting function returned {@code null}
	 * (which usually only occurs when the {@link Value} is incompatible/does not hold the appropriate information)</p>
	 * 
	 * <p>Functions using the converter can use {@link #getTypeName()} to get the name of the type this was trying to convert to, 
	 * in case they are not trying to convert to anything else, where it would be recommended to tell the user the name of
	 * the final type instead.</p>
	 * @param value The {@link Value} to convert
	 * @return The converted value, or {@code null} if the conversion failed in the process
	 * @apiNote <p>While most implementations of this method should and will return the type from this method, 
	 *           implementations that <b>require</b> parameters from {@link #evalAndConvert(Iterator, Context)} or that require multiple
	 *           parameters may decide to throw {@link UnsupportedOperationException} in this method and override {@link #evalAndConvert(Iterator, Context)}
	 *           instead. Those implementations, however, should not be available for map or list types, since those can only operate 
	 *           with {@link Value}.</p>
	 *           <p>Currently, the only implementation that requires that is {@link Params#LAZY_VALUE_IDENTITY} and {@link Params#CONTEXT_PROVIDER}</p>
	 *           <p>Implementations can also provide different implementations for this and {@link #evalAndConvert(Iterator, Context)}, in case
	 *           they can support it in some situations that can't be used else, such as inside of lists or maps, although they should try to provide
	 *           in {@link #evalAndConvert(Iterator, Context)} at least the same conversion as the one from this method.</p>
	 *           <p>Due to the above reasons, {@link ValueConverter} users should try to use {@link #evalAndConvert(Iterator, Context)} whenever
	 *           possible instead of {@link #convert(Value)}, since it allows more flexibility and features.</p>
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
	 * <p>If this {@link ValueConverter} can accepts a variable number of arguments (therefore the result of calling
	 * {@link #consumesVariableArgs()} <b>must</b> return {@code true}), it will return the minimum number of arguments
	 * it will consume.</p>
	 * 
	 * @implNote The default implementation returns {@code 1}
	 * TODO Better name
	 */
	default public int howManyValuesDoesThisEat() {
		return 1;
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
				(Class<R>) annoType.getType(); // We are defining R here.
				// I (altrisi) won't implement generics in varargs. Those are just PAINFUL. They have like 3-4 nested types and don't have the generics
				// and annotations in the same place, plus they have a different "conversion hierarchy" than the rest, making everything require
				// special methods to get the class from type, generics from type and annotations from type. Not worth the effort for me.
				// Those will just fail with a ClassCastException
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
			return (ValueConverter<R>) Params.LAZY_VALUE_IDENTITY;
		if (type == Context.class)
			return (ValueConverter<R>) Params.CONTEXT_PROVIDER;
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
	 * @implSpec <p>Implementations of this method are not required to evaluate the next {@link LazyValue} if they are not supposed to,
	 *           such as in the case of a {@link LazyValue} to {@link LazyValue} identity, neither move the {@link Iterator} to
	 *           the next position, such as in the case of meta providers like {@link Context}.</p>
	 *           <p>Implementations can also evaluate more than a single parameter when being called with this function, 
	 *           but in such case they must implement {@link #howManyValuesDoesThisEat()} to return how many parameters does it
	 *           consume, and, if it accepts variable arguments, implement {@link #consumesVariableArgs()}</p>
	 * @param lazyValueIterator An {@link Iterator} holding the {@link LazyValue} to convert in next position
	 * @param context The {@link Context} to convert with
	 * @return The given {@link LazyValue}, evaluated with the given {@link Context}, and converted to the type {@code <R>} of
	 *         this {@link ValueConverter}
	 */
	default public R evalAndConvert(Iterator<LazyValue> lazyValueIterator, Context context) {
		return convert(lazyValueIterator.next().evalValue(context));
	}
}
