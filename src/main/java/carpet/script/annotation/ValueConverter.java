package carpet.script.annotation;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.ParameterizedType;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.ClassUtils;

import carpet.script.Context;
import carpet.script.annotation.Param.Params;
import carpet.script.value.Value;

import javax.annotation.Nullable;

/**
 * <p>Classes implementing this interface are able to convert {@link Value} instances into {@code <R>}, in order to easily use them in parameters for
 * Scarpet functions created using the {@link ScarpetFunction} annotation.</p>
 *
 * @param <R> The result type that the passed {@link Value}s will be converted to
 */
public interface ValueConverter<R>
{
    /**
     * <p>Returns the the user-friendly name of the result this {@link ValueConverter} converts to, without {@code a} or {@code an}, and without
     * capitalizing the first letter.</p>
     * 
     * <p>This method can return {@code null}, in which case users of this function should hide this {@link ValueConverter} from any aids or usages of
     * the function, meaning that the {@link ValueConverter} is only providing some meta information that isn't directly provided through the Scarpet
     * language.</p>
     * 
     * <p>Those aids calling this method may append an {@code s} to the return value of this method, in case the type is used in places where more
     * than one may be present, such as lists or varargs.</p>
     * 
     * @apiNote This method is intended to only be called when an error has occurred and therefore there is a need to print a stacktrace with some
     *          helpful usage instructions.
     */
    @Nullable
    String getTypeName();

    /**
     * <p>Converts the given {@link Value} to {@code <R>}, which was defined when being registered.</p>
     * 
     * <p> Returns {@code null} if one of the conversions failed, either because the {@link Value} was incompatible in some position of the chain, or
     * because the actual converting function returned {@code null} (which usually only occurs when the {@link Value} is incompatible/does not hold
     * the appropriate information)</p>
     * 
     * <p>Functions using the converter can use {@link #getTypeName()} to get the name of the type this was trying to convert to, in case they are not
     * trying to convert to anything else, where it would be recommended to tell the user the name of the final type instead.</p>
     * 
     * @param value   The {@link Value} to convert
     * @param context The {@link Context} of the call
     * @return The converted value, or {@code null} if the conversion failed in the process
     * @apiNote <p>While most implementations of this method should and will return the type from this method, implementations that <b>require</b>
     *          parameters from {@link #checkAndConvert(Iterator, Context, Context.Type)} or that require multiple parameters may decide to throw
     *          {@link UnsupportedOperationException} in this method and override {@link #checkAndConvert(Iterator, Context, Context.Type)} instead. Those
     *          implementations, however, should not be available for map or list types, since those can only operate with {@link Value}.</p>
     *          <p>Currently, the only implementations requiring that are {@link Params#CONTEXT_PROVIDER} and {@link Params#CONTEXT_TYPE_PROVIDER}</p>
     *          <p>Implementations can also provide different implementations for this and {@link #checkAndConvert(Iterator, Context, Context.Type)}, in case
     *          they can support it in some situations that can't be used else, such as inside of lists or maps, although they should try to provide
     *          in {@link #checkAndConvert(Iterator, Context, Context.Type)} at least the same conversion as the one from this method.</p>
     *          <p>Even with the above reasons, {@link ValueConverter} users should try to implement {@link #convert(Value, Context)} whenever possible instead of
     *          {@link #checkAndConvert(Iterator, Context, Context.Type)}, since it allows its usage in generics of lists and maps.</p>
     */
    @Nullable R convert(Value value, @Nullable Context context);

    /**
     * Old version of {@link #convert(Value)} without taking a {@link Context}.<p>
     * 
     * This shouldn't be used given converters now take a context in the convert function to allow for converting
     * values in lists or other places without using static state.<p>
     * 
     * @param value The value to convert
     * @return A converted value
     * @deprecated Calling this method instead of {@link #convert(Value, Context)} may not return values for some converters
     */
    @Nullable
    @Deprecated(forRemoval = true)
    default R convert(Value value)
    {
        try
        {
            return convert(value, null);
        }
        catch (NullPointerException e)
        {
            return null;
        }
    }

    /**
     * <p>Returns whether this {@link ValueConverter} consumes a variable number of elements from the {@link Iterator} passed to it via
     * {@link #checkAndConvert(Iterator, Context, Context.Type)}.</p>
     * 
     * @implNote The default implementation returns {@code false}
     * @see #valueConsumption()
     */
    default boolean consumesVariableArgs()
    {
        return false;
    }

    /**
     * <p>Declares the number of {@link Value}s this converter consumes from the {@link Iterator} passed to it in
     * {@link #checkAndConvert(Iterator, Context, Context.Type)}.</p>
     * 
     * <p>If this {@link ValueConverter} can accept a variable number of arguments (therefore the result of calling {@link #consumesVariableArgs()}
     * <b>must</b> return {@code true}), it will return the minimum number of arguments it will consume.</p>
     * 
     * @implNote The default implementation returns {@code 1}
     * 
     */
    default int valueConsumption()
    {
        return 1;
    }

    /**
     * <p>Gets the proper {@link ValueConverter} for the given {@link AnnotatedType}, considering the type of {@code R[]} as {@code R}.</p>
     * 
     * <p>This function does not only consider the actual type (class) of the passed {@link AnnotatedType}, but also its annotations and generic
     * parameters in order to get the most specific {@link ValueConverter}.</p>
     * 
     * <p>Some processing is delegated to the appropriate implementations of {@link ValueConverter} in order to get registered converters or generate
     * specific ones for some functions.</p>
     * 
     * @param <R>      The type of the class the returned {@link ValueConverter} will convert to. It is declared from the type in the
     *                 {@link AnnotatedType} directly inside the function.
     * @param annoType The {@link AnnotatedType} to search a {@link ValueConverter}.
     * @return A usable {@link ValueConverter} to convert from a {@link Value} to {@code <R>}
     */
    @SuppressWarnings("unchecked")
    static <R> ValueConverter<R> fromAnnotatedType(AnnotatedType annoType)
    {
        Class<R> type = annoType.getType() instanceof ParameterizedType ? // We are defining R here.
                (Class<R>) ((ParameterizedType) annoType.getType()).getRawType() :
                (Class<R>) annoType.getType();
        // I (altrisi) won't implement generics in varargs. Those are just PAINFUL. They have like 3-4 nested types and don't have the generics
        // and annotations in the same place, plus they have a different "conversion hierarchy" than the rest, making everything require
        // special methods to get the class from type, generics from type and annotations from type. Not worth the effort for me.
        // Example: AnnotatedGenericTypeArray (or similar) being (@Paran.KeyValuePairs Map<String, String>... name)
        // Those will just fail with a ClassCastException.
        if (type.isArray())
        {
            type = (Class<R>) type.getComponentType(); // Varargs
        }
        type = (Class<R>) ClassUtils.primitiveToWrapper(type); // It will be boxed anyway, this saves unboxing-boxing
        if (type == List.class)
        {
            return (ValueConverter<R>) ListConverter.fromAnnotatedType(annoType); // Already checked that type is List
        }
        if (type == Map.class)
        {
            return (ValueConverter<R>) MapConverter.fromAnnotatedType(annoType); // Already checked that type is Map
        }
        if (type == Optional.class)
        {
            return (ValueConverter<R>) OptionalConverter.fromAnnotatedType(annoType);
        }
        if (annoType.getDeclaredAnnotations().length != 0)
        {
            if (annoType.isAnnotationPresent(Param.Custom.class))
            {
                return Params.getCustomConverter(annoType, type); // Throws if incorrect usage
            }
            if (annoType.isAnnotationPresent(Param.Strict.class))
            {
                return (ValueConverter<R>) Params.getStrictConverter(annoType); // Throws if incorrect usage
            }
            if (annoType.getAnnotations()[0].annotationType().getEnclosingClass() == Locator.class)
            {
                return Locator.Locators.fromAnnotatedType(annoType, type);
            }
        }

        // Class only checks
        if (Value.class.isAssignableFrom(type))
        {
            return Objects.requireNonNull(ValueCaster.get(type), "Value subclass " + type + " is not registered. Register it in ValueCaster to use it");
        }
        if (type == Context.class)
        {
            return (ValueConverter<R>) Params.CONTEXT_PROVIDER;
        }
        if (type == Context.Type.class)
        {
            return (ValueConverter<R>) Params.CONTEXT_TYPE_PROVIDER;
        }
        return Objects.requireNonNull(SimpleTypeConverter.get(type), "Type " + type + " is not registered. Register it in SimpleTypeConverter to use it");
    }

    /**
     * <p>Checks for the presence of the next {@link Value} in the given {@link Iterator}, evaluates it with the given {@link Context} and then
     * converts it to this {@link ValueConverter}'s output type.</p>
     * 
     * <p>This should be the preferred way to call the converter, since it <!--allows some conversions that may not be supported by using directly a
     * {@link Value},--> allows multi-param converters and allows meta converters (such as the {@link Context} provider)</p>
     * 
     * @implSpec Implementations of this method are not required to move the {@link Iterator} to the next position, such as in the case of meta
     *           providers like {@link Context}. <p>Implementations can also use more than a single parameter when being called with this function,
     *           but in such case they must implement {@link #valueConsumption()} to return how many parameters do they consume at minimum, and, if
     *           they may consume variable arguments, implement {@link #consumesVariableArgs()}</p> <p>This method holds the same nullability
     *           constraints as {@link #convert(Value, Context)}</p>
     * @param valueIterator An {@link Iterator} holding the {@link Value} to convert in next position
     * @param context       The {@link Context} this function has been called with
     * @param contextType   The {@link Context.Type} that the original function was called with
     * @return The next {@link Value} (s) converted to the type {@code <R>} of this {@link ValueConverter}
     * @implNote This method's default implementation runs the {@link #convert(Value, Context)} function in the next {@link Value} ignoring {@link Context} and
     *           {@code theLazyT}.
     */
    @Nullable
    default R checkAndConvert(Iterator<Value> valueIterator, Context context, Context.Type contextType)
    {
        return !valueIterator.hasNext() ? null : convert(valueIterator.next(), context);
    }
}
