package carpet.script.annotation;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.RegistryAccess;
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

import javax.annotation.Nullable;

/**
 * <p>A converter from a given {@link Object} of type T into a {@link Value}, used in order to convert the outputs of methods into usable Scarpet
 * values.</p>
 *
 * @see #register(Class, BiFunction)
 * @param <T> The type to convert from into a {@link Value}
 */
public final class OutputConverter<T>
{
    private static final Map<Class<?>, OutputConverter<?>> byResult = new HashMap<>();
    private static final OutputConverter<Value> VALUE = new OutputConverter<>((v, regs) -> v);
    static
    {
        // Primitives are handled. Things are boxed in the process anyway, therefore would recommend boxed outputs, so you can use null
        register(Void.TYPE, v -> Value.NULL);
        register(Boolean.class, BooleanValue::of);
        register(Integer.class, (v, r) -> new NumericValue(v));
        register(Double.class, NumericValue::of);
        register(Float.class, NumericValue::of);
        register(Long.class, (v, r) -> new NumericValue(v));
        register(String.class, StringValue::new);
        register(Entity.class, EntityValue::new);
        register(Component.class, FormattedTextValue::new);
        register(Tag.class, NBTSerializableValue::new);
        register(BlockPos.class, (v, r) -> ValueConversions.of(v));
        register(Vec3.class, (v, r) -> ValueConversions.of(v));
        register(ItemStack.class, (v, r) -> ValueConversions.of(v, r));
        register(ResourceLocation.class, (v, r) -> ValueConversions.of(v));
        register(GlobalPos.class, (v, r) -> ValueConversions.of(v));
    }

    private final BiFunction<T, RegistryAccess, Value> converter;

    private OutputConverter(BiFunction<T, RegistryAccess, Value> converter)
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
     * <p>Converts the given input object into a {@link Value}, to be used in return values of Scarpet functions</p>
     *
     * <p>Returns {@link Value#NULL} if passed a {@code null} input</p>
     *
     * @param input The value to convert
     * @return The converted value
     */
    public Value convert(@Nullable T input, RegistryAccess regs)
    {
        return input == null ? Value.NULL : converter.apply(input, regs);
    }

    /**
     * <p>Registers a new type to be able to be used as the return value of methods, converting from inputType to a {@link Value}
     * using the given function.</p>
     *
     * @param <T>       The type of the input type
     * @param inputType The class of T
     * @param converter The function that converts the instance of T to a {@link Value}
     */
    public static <T> void register(Class<T> inputType, BiFunction<T, RegistryAccess, Value> converter)
    {
        OutputConverter<T> instance = new OutputConverter<>(converter);
        if (byResult.containsKey(inputType))
        {
            throw new IllegalArgumentException(inputType + " already has a registered OutputConverter");
        }
        byResult.put(inputType, instance);
    }
    public static <T> void register(Class<T> inputType, Function<T, Value> converter)
    {
        register(inputType, (v, regs) -> converter.apply(v));
    }

    /**
     * @see #register(Class, BiFunction)
     *
     * @deprecated Just use {@link #register(Class, BiFunction)}, it now does the same as this
     */
    @Deprecated
    public static <T> void registerToValue(Class<T> inputType, BiFunction<T, RegistryAccess, Value> converter)
    {
        register(inputType, converter);
    }
    @Deprecated
    public static <T> void registerToValue(Class<T> inputType, Function<T, Value> converter)
    {
        register(inputType, (v, regs) -> converter.apply(v));
    }
}
