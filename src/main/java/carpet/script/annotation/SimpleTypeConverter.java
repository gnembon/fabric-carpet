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

public class SimpleTypeConverter<T extends Value, R> implements ValueConverter<R> {
	private static final Map<Class<?>, SimpleTypeConverter<? extends Value, ?>> byResult = new HashMap<>();
	static {
		registerType(Value.class, ServerPlayerEntity.class, val -> EntityValue.getPlayerByValue(CarpetServer.minecraft_server, val));
		registerType(EntityValue.class, Entity.class, EntityValue::getEntity);
		registerType(Value.class, World.class, val -> ValueConversions.dimFromValue(val, CarpetServer.minecraft_server));
		registerType(Value.class, Text.class, v -> v instanceof FormattedTextValue ? ((FormattedTextValue)v).getText() : new LiteralText(v.getString())); 
		registerType(Value.class, String.class, Value::getString); // Check out @StrictParam for more specific types
		
		// TODO Make sure this doesn't box and unbox primitives. Not sure how to check it, though
		registerType(NumericValue.class, Long.TYPE, NumericValue::getLong);
		registerType(NumericValue.class, Double.TYPE, NumericValue::getDouble);
		registerType(NumericValue.class, Integer.TYPE, NumericValue::getInt);
		registerType(NumericValue.class, Boolean.TYPE, NumericValue::getBoolean); //TODO Strict booleans? Allow any value for boolean by default?
		// Non-primitive versions of the above
		registerType(NumericValue.class, Long.class, NumericValue::getLong);
		registerType(NumericValue.class, Double.class, NumericValue::getDouble);
		registerType(NumericValue.class, Integer.class, NumericValue::getInt);
		registerType(NumericValue.class, Boolean.class, NumericValue::getBoolean);
		//TODO What to do with function/block locators and the like
	}
	
	
	//private final Class<R> outputType;
	private final Function<T, R> converter;
	private final ValueCaster<T> caster;
	
	SimpleTypeConverter(Class<T> inputType, Function<T, R> converter) {
		//this.outputType = outputType;
		this.converter = converter;
		this.caster = ValueCaster.get(inputType);
	}
	
	@Override //TODO This
	public String getTypeName() {
		return caster.getTypeName();
	}
	
	/**
	 * Returns the {@link SimpleTypeConverter} for the specified outputType.
	 * @param <R> The type of the {@link SimpleTypeConverter} you are looking for
	 * @param outputType The class that the returned {@link SimpleTypeConverter} converts to
	 * @return The {@link SimpleTypeConverter} for the specified outputType
	 */
	@SuppressWarnings("unchecked") //Seems safe. T always extends Value, R is always the same as map's key, since map is private. Not 100% sure about T->Value
	public static <R> SimpleTypeConverter<Value, R> get(Class<R> outputType) {
		return (SimpleTypeConverter<Value, R>) byResult.get(outputType);
	}
	
	@Nullable
	public R convert(Value value) {
		T castedValue = caster.convert(value);
		return castedValue == null ? null : converter.apply(castedValue);
	}
	
	public static <T extends Value, R> SimpleTypeConverter<T, R> registerType(Class<T> requiredInputType, Class<R> outputType, Function<T, R> converter) {
		SimpleTypeConverter<T, R> type = new SimpleTypeConverter<>(requiredInputType, converter);
		byResult.put(outputType, type);
		return type;
	}
}
