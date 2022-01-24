package carpet.script.annotation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import carpet.CarpetServer;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.EntityValue;
import carpet.script.value.FormattedTextValue;
import carpet.script.value.NumericValue;
import carpet.script.value.Value;
import carpet.script.value.ValueConversions;

/**
 * <p>A simple {@link ValueConverter} implementation that converts from a specified subclass of {@link Value} into {@code <R>} by using a given
 * function.</p>
 * 
 * <p>{@link SimpleTypeConverter}s are reused whenever asked for one, since they don't have any complexity.</p>
 *
 * @see #registerType(Class, Class, Function, String)
 * 
 * @param <T> The type of the required input {@link Value}
 * @param <R> The type that this converter converts to
 */
public final class SimpleTypeConverter<T extends Value, R> implements ValueConverter<R>
{
    private static final Map<Class<?>, SimpleTypeConverter<? extends Value, ?>> byResult = new HashMap<>();
    static
    {
        registerType(Value.class, ServerPlayer.class, val -> EntityValue.getPlayerByValue(CarpetServer.minecraft_server, val), "online player");
        registerType(EntityValue.class, Entity.class, EntityValue::getEntity, "entity");
        registerType(Value.class, Level.class, val -> ValueConversions.dimFromValue(val, CarpetServer.minecraft_server), "dimension");
        registerType(Value.class, Component.class, FormattedTextValue::getTextByValue, "text");
        registerType(Value.class, String.class, Value::getString, "string"); // Check out @Param.Strict for more specific types

        // Primitives are also query those classes
        registerType(NumericValue.class, Long.class, NumericValue::getLong, "number");
        registerType(NumericValue.class, Double.class, NumericValue::getDouble, "number");
        registerType(NumericValue.class, Integer.class, NumericValue::getInt, "number");
        registerType(Value.class, Boolean.class, Value::getBoolean, "boolean"); // Check out @Param.Strict for more specific types
    }

    private final Function<T, R> converter;
    private final Class<T> valueClass;
    private final String typeName;

    /**
     * <p>The default constructor for {@link SimpleTypeConverter}.</p>
     * 
     * <p>This is public in order to provide an implementation to use when registering {@link ValueConverter}s for the {@link Param.Strict} annotation
     * registry, and it's not intended way to register new {@link SimpleTypeConverter}</p> <p>Use {@link #registerType(Class, Class, Function, String)} for
     * that.</p>
     * 
     * @param inputType The required type for the input {@link Value}
     * @param converter The function to convert an instance of inputType into R.
     */
    public SimpleTypeConverter(Class<T> inputType, Function<T, R> converter, String typeName)
    {
        this.converter = converter;
        this.valueClass = inputType;
        this.typeName = typeName;
    }

    @Override
    public String getTypeName()
    {
        return typeName;
    }

    /**
     * Returns the {@link SimpleTypeConverter} for the specified outputType.
     * 
     * @param <R>        The type of the {@link SimpleTypeConverter} you are looking for
     * @param outputType The class that the returned {@link SimpleTypeConverter} converts to
     * @return The {@link SimpleTypeConverter} for the specified outputType
     */
    @SuppressWarnings("unchecked") // T always extends Value, R is always the same as map's key, since map is private.
    static <R> SimpleTypeConverter<Value, R> get(Class<R> outputType)
    {
        return (SimpleTypeConverter<Value, R>) byResult.get(outputType);
    }

    @Override
    public R convert(Value value)
    {
        return valueClass.isInstance(value) ? converter.apply(valueClass.cast(value)) : null;
    }

    /**
     * <p>Registers a new conversion from a {@link Value} subclass to a Java type.</p>
     * 
     * @param <T> The {@link Value} subtype required for this conversion, for automatic checked casting
     * @param <R> The type of the resulting object
     * @param requiredInputType The {@link Class} of {@code <T>}
     * @param outputType The {@link Class} of {@code <R>>}
     * @param converter A function that converts from the given {@link Value} subtype to the given type. Should ideally return {@code null}
     *                  when given {@link Value} cannot be converted to the {@code <R>}, to follow the {@link ValueConverter} contract, but it
     *                  can also throw an {@link InternalExpressionException} by itself if really necessary.
     * @param typeName The name of the type, following the conventions of {@link ValueConverter#getTypeName()}
     */
    public static <T extends Value, R> void registerType(Class<T> requiredInputType, Class<R> outputType,
            Function<T, R> converter, String typeName)
    {
        SimpleTypeConverter<T, R> type = new SimpleTypeConverter<>(requiredInputType, converter, typeName);
        byResult.put(outputType, type);
    }
}
