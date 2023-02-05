package carpet.script.annotation;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.ClassUtils;

import carpet.script.LazyValue;
import carpet.script.value.EntityValue;
import carpet.script.value.FormattedTextValue;
import carpet.script.value.NBTSerializableValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import carpet.script.value.ValueConversions;

/**
 * <p>A converter from a given {@link Object} of type T into a {@link LazyValue}, used in order to convert the outputs of methods into usable Scarpet
 * values.</p>
 *
 * @see #register(Class, Function)
 * @see #registerToValue(Class, Function)
 * @param <T> The type to convert from into a {@link LazyValue}
 */
public final class OutputConverter<T>
{
    private static final Map<Class<?>, OutputConverter<?>> byResult = new HashMap<>();
    private static final OutputConverter<Value> VALUE = new OutputConverter<>(v -> (c, t) -> v);
    static
    {
        register(LazyValue.class, Function.identity()); // Primitives are handled. Things are boxed in the process anyway, therefore
        register(Boolean.class, v -> (v ? LazyValue.TRUE : LazyValue.FALSE)); // would recommend boxed outputs, so you can use null
        register(Void.TYPE, v -> LazyValue.NULL);
        registerToValue(Integer.class, NumericValue::of);
        registerToValue(Double.class, NumericValue::of);
        registerToValue(Float.class, NumericValue::of);
        registerToValue(Long.class, NumericValue::of);
        registerToValue(String.class, StringValue::new);
        registerToValue(Entity.class, EntityValue::new);
        registerToValue(Component.class, FormattedTextValue::new);
        registerToValue(Tag.class, NBTSerializableValue::new);
        registerToValue(BlockPos.class, ValueConversions::of);
        registerToValue(Vec3.class, ValueConversions::of);
        registerToValue(ItemStack.class, ValueConversions::of);
        registerToValue(ResourceLocation.class, ValueConversions::of);
        registerToValue(GlobalPos.class, ValueConversions::of);
    }

    private final Function<T, LazyValue> converter;

    private OutputConverter(final Function<T, LazyValue> converter)
    {
        this.converter = converter;
    }

    /**
     * <p>Returns the {@link OutputConverter} for the specified returnType.</p>
     * 
     * @param <T>        The type of the {@link OutputConverter} you are looking for
     * @param returnType The class that the returned {@link OutputConverter} converts from
     * @return The {@link OutputConverter} for the specified returnType
     */
    @SuppressWarnings("unchecked") // OutputConverters are stored with their class, for sure since the map is private (&& class has same generic as
                                   // converter)
    public static <T> OutputConverter<T> get(Class<T> returnType)
    {
        if (Value.class.isAssignableFrom(returnType))
        {
            return (OutputConverter<T>) VALUE;
        }
        returnType = (Class<T>) ClassUtils.primitiveToWrapper(returnType); // wrapper holds same generic as primitive: wrapped
        return (OutputConverter<T>) Objects.requireNonNull(byResult.get(returnType),
                "Unregistered output type: " + returnType + ". Register in OutputConverter");
    }

    /**
     * <p>Converts the given input object into a {@link LazyValue}, to be used in return values of Scarpet functions</p>
     * 
     * <p>Returns {@link LazyValue#NULL} if passed a {@code null} input</p>
     * 
     * @param input The value to convert
     * @return The converted value
     */
    public LazyValue convert(final T input)
    {
        return input == null ? LazyValue.NULL : converter.apply(input);
    }

    /**
     * <p>Registers a new type to be able to be used as the return value of methods, converting from inputType to a {@link LazyValue}.</p>
     * 
     * @see #registerToValue(Class, Function)
     * @param <T>       The type of the input type
     * @param inputType The class of T
     * @param converter The function that converts the an instance of T to a {@link LazyValue}
     */
    public static <T> void register(final Class<T> inputType, final Function<T, LazyValue> converter)
    {
        final OutputConverter<T> instance = new OutputConverter<>(converter);
        if (byResult.containsKey(inputType))
        {
            throw new IllegalArgumentException(inputType + " already has a registered OutputConverter");
        }
        byResult.put(inputType, instance);
    }

    /**
     * <p>Registers a new type to be able to be used as the return value of methods, converting from inputType to a {@link LazyValue}, with the
     * converter returning a {@link Value} type.</p>
     * 
     * @see #register(Class, Function)
     * @param <T>       The type of the input type
     * @param inputType The class of T
     * @param converter The function that converts an instance of T to a {@link Value}
     */
    public static <T> void registerToValue(final Class<T> inputType, final Function<T, Value> converter)
    {
        final OutputConverter<T> instance = new OutputConverter<>(converter.andThen(v -> (c, t) -> v));
        if (byResult.containsKey(inputType))
        {
            throw new IllegalArgumentException(inputType + " already has a registered OutputConverter");
        }
        byResult.put(inputType, instance);
    }
}
