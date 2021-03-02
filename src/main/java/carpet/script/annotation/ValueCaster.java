package carpet.script.annotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import carpet.CarpetSettings;
import carpet.script.value.BlockValue;
import carpet.script.value.EntityValue;
import carpet.script.value.FormattedTextValue;
import carpet.script.value.FunctionValue;
import carpet.script.value.ListValue;
import carpet.script.value.MapValue;
import carpet.script.value.NBTSerializableValue;
import carpet.script.value.NullValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.ThreadValue;
import carpet.script.value.Value;

public class ValueCaster<R extends Value> implements ValueConverter<R> {
	private static final Map<Class<? extends Value>, ValueCaster<? extends Value>> byResult = new HashMap<>();
	static {
		register(Value.class, "value");
		register(BlockValue.class, "block");
		register(EntityValue.class, "entity");
		register(FormattedTextValue.class, "formatted text");
		register(FunctionValue.class, "function");
		register(ListValue.class, "list");
		register(MapValue.class, "map");
		register(NBTSerializableValue.class, "nbt object");
		register(NullValue.class, "null"); // Potentially unnecessary
		register(NumericValue.class, "number");
		register(StringValue.class, "string"); // May not be necessary given that everything has getString
		register(ThreadValue.class, "thread");
	}
	
	private final Class<R> outputType;
	private final String typeName;
	
	private ValueCaster(Class<R> outputType, String typeName) {
		this.outputType = outputType;
		this.typeName = typeName;
	}
	
	@Override
	public String getTypeName() {
		return typeName;
	}
	
	/**
	 * Returns the {@link ValueCaster} for the specified outputType.
	 * @param <T> The type of the {@link ValueCaster} you are looking for
	 * @param outputType The class of the {@link Value} the returned {@link ValueCaster} casts to
	 * @return The {@link ValueCaster} for the specified outputType
	 */
	@SuppressWarnings("unchecked") // Casters are stored with their exact class, for sure since the map is private (&& class has same generic as caster)
	public static <T extends Value> ValueCaster<T> get(Class<T> outputType) {
		return (ValueCaster<T>)byResult.get(outputType);
	}
	
	public static <T extends Value> ValueCaster<T> getOrRegister(Class<T> outputType) {
		ValueCaster<T> out = get(outputType);
		if (out != null)
			return get(outputType);
		autoRegister(outputType);
		return get(outputType);
	}
	
	/**
	 * Casts the given {@link Value} to this {@link ValueCaster}'s output type.
	 * 
	 * <p> In case the given {@link Value} isn't an instance of this type, it
	 * returns {@code null}. Functions using the caster can use {@link #getTypeName()}
	 * to get the name of the type this was trying to cast to, in case they are not trying
	 * to cast to anything else, where it would be recommended to tell the user the name of
	 * the final type instead.
	 * @param value The value to cast
	 * @return The value casted to the outputType, or {@code null} if it's not an instance of it
	 * @deprecated Use {@link #convert(Value)}, which is the interface method. Not sure why am I deprecating my own functions that 
	 *             aren't used elsewhere. Maybe since I made a long javadoc for it.
	 */
	@Deprecated
	@Nullable
	public R cast(Value value) {
		if (!outputType.isInstance(value))
			return null; // TODO Check this null on other places
		return outputType.cast(value);
	}
	
	@Override
	@Nullable
	public R convert(Value value) {
		return cast(value);
	}
	
	/**
	 * Registers a new {@link Value} to be able to use it in {@link SimpleTypeConverter}
	 * @param <T> The {@link Value} subclass
	 * @param valueClass The class of T
	 * @param typeName A {@link String} representing the name of this type. It will be used in error messages
	 *                 when there is no higher type required, with the form 
	 *                 <code>(function name) requires a (typeName) to be passed as (argName, if available)</code>
	 */
	public static <T extends Value> void register(Class<T> valueClass, String typeName) {
		ValueCaster<T> caster = new ValueCaster<T>(valueClass, typeName);
		byResult.put(valueClass, caster);
	}
	
	/**
	 * <p>"Heuristically" tries to get the name of a {@link Value} type by running through its constructors,
	 * finding one with a single parameter, and trying to create a new instance of it by passing
	 * a casted null to it everything just to try to call {@link Value#getTypeString()}. Everything 
	 * uglily wrapped into a big try-catch anything to resort to using the class name without {@code Value}
	 * in its name.</p>
	 * 
	 * <p>The worse thing is it works (tested with {@link StringValue})</p>
	 * 
	 * @param <T> The type of the {@link Value} class we are analyzing
	 * @param valueClass The class of the {@link Value}
	 */
	public static <T extends Value> void autoRegister(Class<T> valueClass) {
		try {
			CarpetSettings.LOG.warn("Unregistered Value class provided required in function, '" + valueClass.getName() +"', trying to find its name...");
			if (Modifier.isAbstract(valueClass.getModifiers())) {
				throw new UnsupportedOperationException();
			}
			Constructor<?>[] constructors = valueClass.getConstructors();
			for (Constructor<?> c : constructors) {
				try {
					if (c.getParameterCount() == 1 && Modifier.isPublic(c.getModifiers())) {
						Class<?> paramClass = c.getParameters()[0].getClass();
						Object castedNull = paramClass.cast(null);
						Value val = (Value)c.newInstance(castedNull);
						register(valueClass, val.getTypeString());
					}
				} catch (Exception e) {} // Try next
			}
			throw new UnsupportedOperationException();
		} catch (Exception e) { //Heuristics failed. Just use class name
			register(valueClass, valueClass.getCanonicalName().toLowerCase(Locale.ROOT).replace("value", ""));
			CarpetSettings.LOG.warn("Couldn't automagically get type name for '"+valueClass.getName()+"', using class without 'value'. Please register it");
		}
	}
}
