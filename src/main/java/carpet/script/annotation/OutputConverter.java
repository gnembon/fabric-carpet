package carpet.script.annotation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import carpet.script.LazyValue;
import carpet.script.value.EntityValue;
import carpet.script.value.FormattedTextValue;
import carpet.script.value.NBTSerializableValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import carpet.script.value.ValueConversions;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.Tag;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.GlobalPos;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class OutputConverter<T> {
	private static final Map<Class<?>, OutputConverter<?>> byResult = new HashMap<>();
	static {
		register(LazyValue.class, Function.identity());
		register(Value.class, v -> (c, t) -> v);
		register(Boolean.TYPE, v -> (v ? LazyValue.TRUE : LazyValue.FALSE)); // Boxed and unboxed. Things are boxed in the process anyway, therefore
		register(Boolean.class, v -> (v ? LazyValue.TRUE : LazyValue.FALSE));                 // would recommend boxed outputs, so you can use null
		register(Integer.TYPE, v -> (c, t) -> NumericValue.of(v));
		register(Integer.class, v -> (c, t) -> NumericValue.of(v));
		register(Double.TYPE, v -> (c, t) -> NumericValue.of(v));
		register(Double.class, v -> (c, t) -> NumericValue.of(v));
		register(Float.TYPE, v -> (c, t) -> NumericValue.of(v));
		register(Float.class, v -> (c, t) -> NumericValue.of(v));
		register(Long.TYPE, v -> (c, t) -> NumericValue.of(v));
		register(Long.class, v -> (c, t) -> NumericValue.of(v));
		register(String.class, v -> (c, t) -> new StringValue(v));
		register(Entity.class, v -> (c, t) -> new EntityValue(v));
		register(Text.class, v -> (c, t) -> new FormattedTextValue(v));
		register(Tag.class, v -> (c, t) -> new NBTSerializableValue(v));
		register(BlockPos.class, v -> (c, t) -> ValueConversions.of(v));
		register(Vec3d.class, v -> (c, t) -> ValueConversions.of(v));
		register(ItemStack.class, v -> (c, t) -> ValueConversions.of(v));
		register(Identifier.class, v -> (c, t) -> ValueConversions.of(v));
		register(GlobalPos.class, v -> (c, t) -> ValueConversions.of(v));
	}
	
	private final Function<T, LazyValue> converter;
	
	private OutputConverter(Function<T, LazyValue> converter) {
		this.converter = converter;
	}
	
	
	/**
	 * Returns the {@link OutputConverter} for the specified returnType.
	 * @param <T> The type of the {@link OutputConverter} you are looking for
	 * @param returnType The class that the returned {@link OutputConverter} converts from
	 * @return The {@link OutputConverter} for the specified outputType
	 */
	@SuppressWarnings("unchecked") // OutputConverters are stored with their class, for sure since the map is private (&& class has same generic as converter)
	public static <T> OutputConverter<T> get(Class<T> returnType) {
		return (OutputConverter<T>) byResult.get(returnType);
	}
	
	/**
	 * Converts the given input object into a {@link LazyValue}, to be used in return values 
	 * of Scarpet functions
	 * @param input The value to convert
	 * @return The converted value
	 */
	public LazyValue convert(T input) {
		return input == null ? LazyValue.NULL : converter.apply(input);
	}
	
	/**
	 * Registers a new type to be able to use it as the return value of methods
	 * @param <T> The type of the return value
	 * @param outputClass The class of T
	 * @param converter The function that converts the an instance of T to a {@link LazyValue}
	 */
	public static <T> void register(Class<T> outputClass, Function<T, LazyValue> converter) {
		OutputConverter<T> instance = new OutputConverter<T>(converter);
		byResult.putIfAbsent(outputClass, instance);
	}
}
