package carpet.script.annotation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import carpet.CarpetServer;
import carpet.script.value.EntityValue;
import carpet.script.value.FormattedTextValue;
import carpet.script.value.NumericValue;
import carpet.script.value.Value;
import carpet.script.value.ValueConversions;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.world.World;

/**
 * <p>A simple {@link ValueConverter} implementation that converts from a specified subclass of {@link Value} into {@code <R>}
 * by using a given function.</p>
 * 
 * <p>This class uses a {@link ValueCaster} in order to check and cast the {@link Value} to its required type, and then converts it
 * using the given function.</p>
 *
 * @see #registerType(Class, Class, Function)
 * 
 * @param <T> The type of the required input {@link Value}
 * @param <R> The type that this converter converts to
 */
public final class SimpleTypeConverter<T extends Value, R> implements ValueConverter<R> {
	private static final Map<Class<?>, SimpleTypeConverter<? extends Value, ?>> byResult = new HashMap<>();
	static {
		registerType(Value.class, ServerPlayerEntity.class, val -> EntityValue.getPlayerByValue(CarpetServer.minecraft_server, val), "online player");
		registerType(EntityValue.class, Entity.class, EntityValue::getEntity, "entity");
		registerType(Value.class, World.class, val -> ValueConversions.dimFromValue(val, CarpetServer.minecraft_server), "dimension");
		registerType(Value.class, Text.class, v -> v instanceof FormattedTextValue ? ((FormattedTextValue)v).getText() : new LiteralText(v.getString()), "text"); 
		registerType(Value.class, String.class, Value::getString, "string"); // Check out @Param.Strict for more specific types
		
		// TODO Make sure this doesn't box and unbox primitives. Not sure how to check it, though. EDIT: I think they do, and have to
		registerType(NumericValue.class, Long.TYPE, NumericValue::getLong, "number");
		registerType(NumericValue.class, Double.TYPE, NumericValue::getDouble, "number");
		registerType(NumericValue.class, Integer.TYPE, NumericValue::getInt, "number");
		registerType(Value.class, Boolean.TYPE, Value::getBoolean, "boolean"); // Check out @Param.Strict for more specific types
		// Non-primitive versions of the above
		registerType(NumericValue.class, Long.class, NumericValue::getLong, "number");
		registerType(NumericValue.class, Double.class, NumericValue::getDouble, "number");
		registerType(NumericValue.class, Integer.class, NumericValue::getInt, "number");
		registerType(Value.class, Boolean.class, Value::getBoolean, "boolean"); // Check out @Param.Strict for more specific types
	}
	
	private final Function<T, R> converter;
	private final ValueCaster<T> caster;
	private final String typeName;
	
	/**
	 * <p>The default constructor for {@link SimpleTypeConverter}.</p>
	 * 
	 * <p>This is public in order to provide an implementation to use when registering {@link ValueConverter}s
	 * for the {@link Param.Strict} annotation registry, and it's not intended way to register new {@link SimpleTypeConverter}</p>
	 * <p>Use {@link #registerType(Class, Class, Function)} for that.</p>
	 * 
	 * @param inputType The required type for the input {@link Value}
	 * @param converter The function to convert an instance of inputType into R.
	 */
	public SimpleTypeConverter(Class<T> inputType, Function<T, R> converter, String typeName) {
		this.converter = converter;
		this.caster = ValueCaster.get(inputType);
		this.typeName = typeName;
	}
	
	@Override
	public String getTypeName() {
		return typeName;
	}
	
	/**
	 * Returns the {@link SimpleTypeConverter} for the specified outputType.
	 * @param <R> The type of the {@link SimpleTypeConverter} you are looking for
	 * @param outputType The class that the returned {@link SimpleTypeConverter} converts to
	 * @return The {@link SimpleTypeConverter} for the specified outputType
	 */
	@SuppressWarnings("unchecked") //Seems safe. T always extends Value, R is always the same as map's key, since map is private. Not 100% sure about T->Value
	static <R> SimpleTypeConverter<Value, R> get(Class<R> outputType) {
		return (SimpleTypeConverter<Value, R>) byResult.get(outputType);
	}
	
	@Override
	@Nullable
	public R convert(Value value) {
		T castedValue = caster.convert(value);
		return castedValue == null ? null : converter.apply(castedValue);
	}
	
	public static <T extends Value, R> SimpleTypeConverter<T, R> registerType(Class<T> requiredInputType, Class<R> outputType, Function<T, R> converter, String typeName) {
		SimpleTypeConverter<T, R> type = new SimpleTypeConverter<>(requiredInputType, converter, typeName);
		byResult.put(outputType, type);
		return type;
	}
}
