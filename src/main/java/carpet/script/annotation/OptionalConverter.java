package carpet.script.annotation;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Optional;

import carpet.script.Context;
import carpet.script.value.Value;

import javax.annotation.Nullable;

/**
 * <p>{@link ValueConverter} that accepts a parameter to not be present on function call.</p>
 * 
 * <p>Note that it will not work properly if it's not at the end of the function, since values are consumed in an ordered way, therefore it will throw
 * if the value is not present since it will try to evaluate the next parameter into {@code <R>} and horribly fail, or it will draw values from the
 * {@link ValueConverter}s after it, horribly failing too..</p>
 * 
 * <p>Why {@link Optional}?</p>
 * 
 * <p>It allows passing three states:</p>
 * <ul>
 *  <li>A wrapped object, if the value was present and correct</li>
 *  <li>An incorrect value ({@code null}), if the value was present but incorrect, as per the contract of {@link ValueConverter} and the way
 * {@link AnnotationParser} will consider {@link null} values</li>
 *  <li>An empty {@link Optional}, if the value was not present (or {@code null})</li>
 * </ul>
 *
 * @param <R> The type of the internal {@link ValueConverter}, basically the generic type of the {@link Optional}
 */
final class OptionalConverter<R> implements ValueConverter<Optional<R>>
{
    private final ValueConverter<R> typeConverter;

    @Override
    public String getTypeName()
    {
        return "optional " + typeConverter.getTypeName();
    }

    private OptionalConverter(AnnotatedType type)
    {
        typeConverter = ValueConverter.fromAnnotatedType(type);
    }

    /**
     * {@inheritDoc}
     * 
     * @implNote Unlike most other converters, {@link OptionalConverter} will not call this method from
     *           {@link #checkAndConvert(Iterator, Context, Context.Type)} and is only used as a fallback in types that don't support it.
     */
    @Nullable
    @Override
    public Optional<R> convert(Value value, @Nullable Context context)
    {
        if (value.isNull())
        {
            return Optional.empty();
        }
        R converted = typeConverter.convert(value, context);
        if (converted == null)
        {
            return null;
        }
        return Optional.of(converted);
    }

    @Nullable
    @Override
    public Optional<R> checkAndConvert(Iterator<Value> valueIterator, Context context, Context.Type theLazyT)
    {
        if (!valueIterator.hasNext() || valueIterator.next().isNull())
        {
            return Optional.empty();
        }
        ((ListIterator<Value>) valueIterator).previous();
        R converted = typeConverter.checkAndConvert(valueIterator, context, theLazyT);
        if (converted == null)
        {
            return null;
        }
        return Optional.of(converted);
    }

    @Override
    public boolean consumesVariableArgs()
    {
        return true;
    }

    @Override
    public int valueConsumption()
    {
        return 0; // Optional parameters therefore require a minimum of 0
    }

    /**
     * <p>Returns a new {@link OptionalConverter} to convert to the type in the given {@link AnnotatedType} if there is at least one element left in
     * the iterator, with any parameters, annotations or anything that was present in there.</p>
     * 
     * <p>The given {@link ValueConverter} will be called after ensuring that there is at least one element left in the iterator to the type specified
     * in the generics of this {@link AnnotatedType}, or return an empty {@link Optional} if there is nothing left or the first value meets
     * {@link Value#isNull()}.
     * 
     * @apiNote This method expects the {@link AnnotatedType} to already be of {@link Optional} type, and, while it will technically accept a
     *          non-{@link Optional} {@link AnnotatedType}, it will fail with an {@link ArrayIndexOutOfBoundsException} if it doesn't has at least one
     *          generic parameter.
     * @param annotatedType The type to get generics information from
     * @return A new {@link OptionalConverter} for the data specified in the {@link AnnotatedType}
     */
    static OptionalConverter<?> fromAnnotatedType(AnnotatedType annotatedType)
    {
        AnnotatedParameterizedType paramType = (AnnotatedParameterizedType) annotatedType;
        AnnotatedType wrappedType = paramType.getAnnotatedActualTypeArguments()[0];
        return new OptionalConverter<>(wrappedType);
    }
}
