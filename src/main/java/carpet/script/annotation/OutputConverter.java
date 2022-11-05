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

import carpet.script.value.BooleanValue;
import carpet.script.value.EntityValue;
import carpet.script.value.FormattedTextValue;
import carpet.script.value.NBTSerializableValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import carpet.script.value.ValueConversions;

/**
 * <p>A converter from a given {@link Object} of type T into a {@link Value}, used in order to convert the outputs of methods into usable Scarpet
 * values.</p>
 *
 * @see #register(Class, Function)
 * @param <T> The type to convert from into a {@link Value}
 */
public final class OutputConverter<T>
{
    private static final Map<Class<?>, OutputConverter<?>> byResult = new HashMap<>();
    private static final OutputConverter<Value> VALUE = new OutputConverter<>(Function.identity());
    static
    {
        // Primitives are handled. Things are boxed in the process anyway, therefore would recommend boxed outputs, so you can use null
        register(Void.TYPE, v -> Value.NULL);
        register(Boolean.class, BooleanValue::of);
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

    private final Function<T, Value> converter;

    private OutputConverter(Function<T, Value> converter)
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
            return (OutputConverter<T>) VALUE;
        returnType = (Class<T>) ClassUtils.primitiveToWrapper(returnType); // wrapper holds same generic as primitive: wrapped
        return (OutputConverter<T>) Objects.requireNonNull(byResult.get(returnType),
                "Unregistered output type: " + returnType + ". Register in OutputConverter");
    }

    /**
     * <p>Converts the given input object into a {@link Value}, to be used in return values of Scarpet functions</p>
     * 
     * <p>Returns {@link Value#NULL} if passed a {@code null} input</p>
     * 
     * @param input The value to convert
     * @return The converted value
     */
    public Value convert(T input)
    {
        return input == null ? Value.NULL : converter.apply(input);
    }

    /**
     * <p>Registers a new type to be able to be used as the return value of methods, converting from inputType to a {@link Value}
     * using the given function.</p>
     * 
     * @param <T>       The type of the input type
     * @param inputType The class of T
     * @param converter The function that converts the instance of T to a {@link Value}
     */
    public static <T> void register(Class<T> inputType, Function<T, Value> converter)
    {
        OutputConverter<T> instance = new OutputConverter<T>(converter);
        if (byResult.containsKey(inputType))
            throw new IllegalArgumentException(inputType + " already has a registered OutputConverter");
        byResult.put(inputType, instance);
    }

    /**
     * @see #register(Class, Function)
     * 
     * @deprecated Just use {@link #register(Class, Function)}, it now does the same as this
     */
    @Deprecated
    public static <T> void registerToValue(Class<T> inputType, Function<T, Value> converter)
    {
        register(inputType, converter);
    }
}
