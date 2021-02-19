package carpet.script.annotation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import carpet.CarpetServer;
import carpet.script.value.EntityValue;
import carpet.script.value.FormattedTextValue;
import carpet.script.value.ListValue;
import carpet.script.value.MapValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import carpet.script.value.ValueConversions;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;

public class SimpleTypeConverter<T extends Value, R> implements ValueConverter<R> { //TODO remove all the assignments, just register
	private static final Map<Class<?>, SimpleTypeConverter<? extends Value, ?>> byResult = new HashMap<>();
	public static final SimpleTypeConverter<EntityValue, ServerPlayerEntity> SERVER_PLAYER_ENTITY = 
			registerType(EntityValue.class, ServerPlayerEntity.class, val -> EntityValue.getPlayerByValue(CarpetServer.minecraft_server, val));
	public static final SimpleTypeConverter<EntityValue, Entity> ENTITY = registerType(EntityValue.class, Entity.class, EntityValue::getEntity);
	public static final SimpleTypeConverter<Value, World> WORLD = registerType(Value.class, World.class, val -> ValueConversions.dimFromValue(val, CarpetServer.minecraft_server));
	public static final SimpleTypeConverter<FormattedTextValue, Text> TEXT = registerType(FormattedTextValue.class, Text.class, FormattedTextValue::getText);
	public static final SimpleTypeConverter<StringValue, String> STRING = registerType(StringValue.class, String.class, StringValue::getString);
	// TODO Make this complex converting contents (to the <Generics>). Move outside of here. The generics conversions should use this class though 
	public static final SimpleTypeConverter<ListValue, List> LIST = registerType(ListValue.class, List.class, ListValue::getItems);
	public static final SimpleTypeConverter<MapValue, Map> MAP = registerType(MapValue.class, Map.class, MapValue::getMap);
	// TODO Make sure this doesn't box and unbox primitives. Not sure how to check it, though
	public static final SimpleTypeConverter<NumericValue, Long> LONG = registerType(NumericValue.class, Long.TYPE, NumericValue::getLong);
	public static final SimpleTypeConverter<NumericValue, Double> DOUBLE = registerType(NumericValue.class, Double.TYPE, NumericValue::getDouble);
	public static final SimpleTypeConverter<NumericValue, Integer> INT = registerType(NumericValue.class, Integer.TYPE, NumericValue::getInt);
	//TODO What to do with function/block locators and the like
	
	
	//private final Class<R> outputType;
	private final Function<T, R> converter;
	private final ValueCaster<T> caster;
	
	private SimpleTypeConverter(Class<T> inputType, Class<R> outputType, Function<T, R> converter) {
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
	 * @param <T> The type of the {@link SimpleTypeConverter} you are looking for
	 * @param outputType The class that the returned {@link SimpleTypeConverter} converts to
	 * @return The {@link SimpleTypeConverter} for the specified outputType
	 */
	@SuppressWarnings("unchecked") //Seems safe. T always extends Value, R is always the same as map's key, since map is private. Not 100% sure about T->Value
	public static <R> SimpleTypeConverter<Value, R> get(Class<R> outputType) {
		return (SimpleTypeConverter<Value, R>) byResult.get(outputType);
	}
	
	@Nullable
	public R convert(Value value) {
		T castedValue = caster.cast(value);
		return castedValue == null ? null : converter.apply(castedValue);
	}
	
	/* This would be quite unchecked given how we cast generics on #get()
	public R convert(T val) {
		return converter.apply(val);
	}
	*/
	
	public static <T extends Value, R> SimpleTypeConverter<T, R> registerType(Class<T> requiredInputType, Class<R> outputType, Function<T, R> converter) {
		SimpleTypeConverter<T, R> type = new SimpleTypeConverter<T, R>(requiredInputType, outputType, converter);
		byResult.put(outputType, type);
		return type;
	}
}
