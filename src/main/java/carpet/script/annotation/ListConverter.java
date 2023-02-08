package carpet.script.annotation;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import carpet.script.Context;
import carpet.script.value.ListValue;
import carpet.script.value.Value;

import javax.annotation.Nullable;

/**
 * <p>Converts a given {@link ListValue} into a {@link List} of values converted to {@code <T>}.</p>
 * 
 * <p>If the {@link Param.AllowSingleton} annotation is specified, allows creating a singleton from
 * a loose element compatible with the type conversion.</p> 
 * 
 * <p>Lists provided by this converter are <b>not</b> linked to the initial list, and therefore will not
 * reflect changes in either of them</p>
 *
 * @param <T> The type of the element that will be inside the list
 */
final class ListConverter<T> implements ValueConverter<List<T>>
{
    private final ValueConverter<T> itemConverter;
    private final boolean allowSingletonCreation;

    @Override
    public String getTypeName()
    {
        return (allowSingletonCreation ? itemConverter.getTypeName() + " or " : "") + "list of " + itemConverter.getTypeName() + "s";
    }

    @Nullable
    @Override
    public List<T> convert(Value value, @Nullable Context context)
    {
        return value instanceof ListValue ? convertListValue((ListValue) value, context) : allowSingletonCreation ? convertSingleton(value, context) : null;
    }

    @Nullable
    private List<T> convertListValue(ListValue values, @Nullable Context context)
    {
        List<T> list = new ArrayList<>(values.getItems().size());
        for (Value value : values)
        {
            T converted = itemConverter.convert(value, context);
            if (converted == null)
            {
                return null;
            }
            list.add(converted);
        }
        return list;
    }

    @Nullable
    private List<T> convertSingleton(Value val, @Nullable Context context)
    {
        T converted = itemConverter.convert(val, context);
        if (converted == null)
        {
            return null;
        }
        return Collections.singletonList(converted);

    }

    private ListConverter(AnnotatedType itemType, boolean allowSingletonCreation)
    {
        itemConverter = ValueConverter.fromAnnotatedType(itemType);
        this.allowSingletonCreation = allowSingletonCreation;
    }

    /**
     * <p>Returns a new {@link ListConverter} to convert to the given {@link AnnotatedType}.</p>
     * 
     * <p>The returned {@link ValueConverter} will convert the objects inside the list to the
     * generics specified in the {@link AnnotatedType}, and the {@link ValueConverter} will
     * be set to accept non-list (but correct) items and make a singleton out of them
     * if the {@link Param.AllowSingleton} annotation has been specified.</p>
     * 
     * @apiNote This method expects the {@link AnnotatedType} to already be of {@link List} type, and, while it will
     *          technically accept a non-{@link List} {@link AnnotatedType}, it will fail with an {@link ArrayIndexOutOfBoundsException}
     *          if it doesn't has at least one generic parameter. 
     * @param annotatedType The type to get generics information from
     * @return A new {@link ListConverter} for the data specified in the {@link AnnotatedType}
     */
    static ListConverter<?> fromAnnotatedType(AnnotatedType annotatedType)
    {
        AnnotatedParameterizedType paramType = (AnnotatedParameterizedType) annotatedType;
        AnnotatedType itemType = paramType.getAnnotatedActualTypeArguments()[0];
        boolean allowSingletonCreation = annotatedType.isAnnotationPresent(Param.AllowSingleton.class);
        return new ListConverter<>(itemType, allowSingletonCreation);
    }

}
