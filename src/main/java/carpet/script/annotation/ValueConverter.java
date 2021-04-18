package carpet.script.annotation;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.ParameterizedType;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.ClassUtils;
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
	 * <p>Returns the the user-friendly name of the result this {@link ValueConverter} converts to, without {@code a} or {@code an},
	 * and without capitalizing the first letter.</p>
	 * 
	 * <p>This method can return {@code null}, in which case users of this function should hide this {@link ValueConverter}
	 * from any aids or usages of the function, meaning that the {@link ValueConverter} is only providing some meta information
	 * that isn't directly provided through the Scarpet language.</p>
	 * 
	 * <p>Those aids calling this method may append an {@code s} to the return value of this method, in case the type is used
	 * in places where more than one may be present, such as lists or varargs.</p>
	 * 
	 * @apiNote This method is intended to only be called when an error has occurred and therefore there is a need to print a 
	 *           stacktrace with some helpful usage instructions.
	 */
	public String getTypeName();

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
	 *           implementations that <b>require</b> parameters from {@link #evalAndConvert(Iterator, Context, Integer)} or that require multiple
	 *           parameters may decide to throw {@link UnsupportedOperationException} in this method and override {@link #evalAndConvert(Iterator, Context, Integer)}
	 *           instead. Those implementations, however, should not be available for map or list types, since those can only operate 
	 *           with {@link Value}.</p>
	 *           <p>Currently, the only implementations requiring that are {@link Params#LAZY_VALUE_IDENTITY} and {@link Params#CONTEXT_PROVIDER}</p>
	 *           <p>Implementations can also provide different implementations for this and {@link #evalAndConvert(Iterator, Context, Integer)}, in case
	 *           they can support it in some situations that can't be used else, such as inside of lists or maps, although they should try to provide
	 *           in {@link #evalAndConvert(Iterator, Context, Integer)} at least the same conversion as the one from this method.</p>
	 *           <p>Even with the above reasons, {@link ValueConverter} users should try to implement {@link #convert(Value)} whenever
	 *           possible instead of {@link #evalAndConvert(Iterator, Context, Integer)}, since it allows its usage in generics of lists and maps.</p>
	 */
	@Nullable
	public R convert(Value value);
	
	/**
	 * <p>Returns whether this {@link ValueConverter} consumes a variable number of elements from the {@link Iterator}
	 * passed to it via {@link #evalAndConvert(Iterator, Context, Integer)}.</p>
	 * @implNote The default implementation returns {@code false} 
	 * @see #valueConsumption()
	 */
	default public boolean consumesVariableArgs() {
		return false;
	}
	
	/**
	 * <p>Declares the number of {@link LazyValue}s this method consumes from the {@link Iterator} passed to it in
	 * {@link #evalAndConvert(Iterator, Context, Integer)}.</p>
	 * 
	 * <p>If this {@link ValueConverter} can accept a variable number of arguments (therefore the result of calling
	 * {@link #consumesVariableArgs()} <b>must</b> return {@code true}), it will return the minimum number of arguments
	 * it will consume.</p>
	 * 
	 * @implNote The default implementation returns {@code 1}
	 * 
	 */
	default public int valueConsumption() {
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
	@SuppressWarnings("unchecked")
	public static <R> ValueConverter<R> fromAnnotatedType(AnnotatedType annoType) {
		Class<R> type = annoType.getType() instanceof ParameterizedType ?  // We are defining R here.
				(Class<R>) ((ParameterizedType)annoType.getType()).getRawType() :
				(Class<R>) annoType.getType();
				// I (altrisi) won't implement generics in varargs. Those are just PAINFUL. They have like 3-4 nested types and don't have the generics
				// and annotations in the same place, plus they have a different "conversion hierarchy" than the rest, making everything require
				// special methods to get the class from type, generics from type and annotations from type. Not worth the effort for me.
				// Example: AnnotatedGenericTypeArray (or similar) being (@Paran.KeyValuePairs Map<String, String>... name)
				// Those will just fail with a ClassCastException.
		if (type.isArray()) type = (Class<R>) type.getComponentType(); // Varargs
		type = (Class<R>) ClassUtils.primitiveToWrapper(type); // It will be boxed anyway, this saves unboxing-boxing
		if (type == List.class)
			return (ValueConverter<R>) ListConverter.fromAnnotatedType(annoType); //Already checked that type is List
		if (type == Map.class)
			return (ValueConverter<R>) MapConverter.fromAnnotatedType(annoType);  //Already checked that type is Map
		if (type == Optional.class)
			return (ValueConverter<R>) OptionalConverter.fromAnnotatedType(annoType);
		if (annoType.getDeclaredAnnotations().length != 0) {
			if (annoType.isAnnotationPresent(Param.Custom.class))
				return Param.Params.getCustomConverter(annoType, type); // Throws if incorrect usage 
			if (annoType.isAnnotationPresent(Param.Strict.class))
				return (ValueConverter<R>)Params.getStrictConverter(annoType); // Throws if incorrect usage
			if (annoType.isAnnotationPresent(Param.TheLazyT.class)) {
				if (type != Integer.class)
					throw new IllegalArgumentException("The lazy T can only be used in Integer parameters");
				return (ValueConverter<R>) Params.LAZY_T_PROVIDER;
			}
			if (annoType.getAnnotations()[0].annotationType().getEnclosingClass() == Locator.class)
				return Locator.Locators.fromAnnotatedType(annoType, type);
		}
		
		//Start: Old fromType.
		if (type.isAssignableFrom(Value.class))
			return Objects.requireNonNull(ValueCaster.get(type), "Value subclass " + type + " is not registered. Register it in ValueCaster to use it");
		if (type == LazyValue.class)
			return (ValueConverter<R>) Params.LAZY_VALUE_IDENTITY;
		if (type == Context.class)
			return (ValueConverter<R>) Params.CONTEXT_PROVIDER;
		return Objects.requireNonNull(SimpleTypeConverter.get(type), "Type " + type + " is not registered. Register it in SimpleTypeConverter to use it");
	}
	
	/**
	 * <p>Checks for the presence of the next {@link LazyValue} in the given {@link Iterator}, evaluates it with the given {@link Context} and 
	 * then converts it to this {@link ValueConverter}'s output type.</p>
	 * 
	 * <p>This should be the preferred way to call the converter, since it allows some conversions that
	 * may not be supported by using directly a {@link Value}, allows multi-param converters and allows
	 * meta converters (such as the {@link Context} provider)</p>
	 * 
	 * @implSpec Implementations of this method are not required to evaluate the next {@link LazyValue} if they are not supposed to,
	 *           such as in the case of a {@link LazyValue} to {@link LazyValue} identity, neither move the {@link Iterator} to
	 *           the next position, such as in the case of meta providers like {@link Context}.
	 *           <p>Implementations can also evaluate more than a single parameter when being called with this function, 
	 *           but in such case they must implement {@link #valueConsumption()} to return how many parameters do they
	 *           consume at minimum, and, if they may consume variable arguments, implement {@link #consumesVariableArgs()}</p>
	 *           <p>This method holds the same nullability constraints as {@link #convert(Value)}</p>
	 * @param lazyValueIterator An {@link Iterator} holding the {@link LazyValue} to convert in next position
	 * @param context The {@link Context} to convert with
	 * @param theLazyT The {@code t} that the original function was called with. It is ignored by the default implementation. 
	 * @return The given {@link LazyValue}, evaluated with the given {@link Context}, and converted to the type {@code <R>} of
	 *         this {@link ValueConverter}
	 * @implNote This method's default implementation runs the {@link #convert(Value)} function in the next {@link LazyValue} evaluated with
	 *          the given context, ignoring {@code theLazyT}.
	 */
	default public R evalAndConvert(Iterator<LazyValue> lazyValueIterator, Context context, Integer theLazyT) {
		if (!lazyValueIterator.hasNext())
			return null;
		return convert(lazyValueIterator.next().evalValue(context));
	}
}
