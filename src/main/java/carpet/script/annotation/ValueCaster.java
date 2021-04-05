package carpet.script.annotation;

import java.util.HashMap;
import java.util.Map;

import carpet.script.value.AbstractListValue;
import carpet.script.value.BlockValue;
import carpet.script.value.BooleanValue;
import carpet.script.value.EntityValue;
import carpet.script.value.FormattedTextValue;
import carpet.script.value.FunctionValue;
import carpet.script.value.ListValue;
import carpet.script.value.MapValue;
import carpet.script.value.NBTSerializableValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.ThreadValue;
import carpet.script.value.Value;

public final class ValueCaster<R> implements ValueConverter<R> {
	private static final Map<Class<? extends Value>, ValueCaster<? extends Value>> byResult = new HashMap<>();
	static {
		register(Value.class, "value");
		register(BlockValue.class, "block");
		register(EntityValue.class, "entity");
		register(FormattedTextValue.class, "formatted text");
		register(FunctionValue.class, "function");
		register(ListValue.class, "list");
		register(MapValue.class, "map");
		register(AbstractListValue.class, "list or similar"); // For LazyListValue basically? Not sure what should use this
		register(NBTSerializableValue.class, "nbt object");
		register(NumericValue.class, "number");
		register(BooleanValue.class, "boolean");
		register(StringValue.class, "string");
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
	 * Returns the registered {@link ValueCaster} for the specified outputType.
	 * @param <R> The type of the {@link ValueCaster} you are looking for
	 * @param outputType The class of the {@link Value} the returned {@link ValueCaster} casts to
	 * @return The {@link ValueCaster} for the specified outputType
	 */
	@SuppressWarnings("unchecked") // Casters are stored with their exact class, for sure since the map is private (&& class has same generic as caster)
	public static <R> ValueCaster<R> get(Class<R> outputType) {
		return (ValueCaster<R>)byResult.get(outputType);
	}
	
	@Override
	public R convert(Value value) {
		if (!outputType.isInstance(value))
			return null;
		return outputType.cast(value);
	}
	
	/**
	 * Registers a new {@link Value} to be able to use it in {@link SimpleTypeConverter}
	 * @param <R> The {@link Value} subclass
	 * @param valueClass The class of T
	 * @param typeName A {@link String} representing the name of this type. It will be used in error messages
	 *                 when there is no higher type required, with the form 
	 *                 <code>(function name) requires a (typeName) to be passed as (argName, if available)</code>
	 */
	public static <R extends Value> void register(Class<R> valueClass, String typeName) {
		ValueCaster<R> caster = new ValueCaster<R>(valueClass, typeName);
		byResult.putIfAbsent(valueClass, caster);
	}
	

	/**
	 * Returns a {@link ValueCaster} for the specified outputType. In case it isn't registered,
	 * tries to generate one, using (slightly concerning) reflection hacks to get its user-friendly name.
	 * @param <R> The type of the {@link ValueCaster} you are looking for
	 * @param outputType The class of the {@link Value} the returned {@link ValueCaster} casts to
	 * @return A {@link ValueCaster} for the specified outputType
	 */
	/* This was the holder for autoRegister, that is ScheduledForRemoval
	public static <R> ValueCaster<R> getOrRegister(Class<R> outputType) {
		ValueCaster<R> out = get(outputType);
		if (out != null)
			return out;
		autoRegister((Class<? extends Value>)outputType); // TODO yeet this method
		return get(outputType);
	}*/
	
	/**
	 * <p>"Heuristically" tries to get the name of a {@link Value} type by running through its constructors,
	 * finding one with a single parameter, and trying to create a new instance of it by passing
	 * a casted null to it everything just to try to call {@link Value#getTypeString()}. Everything 
	 * uglily wrapped into a big try-catch anything to resort to using the class name without {@code Value}
	 * in its name.</p>
	 * 
	 * <p>The worse thing is it works (tested with {@link StringValue})</p>
	 * 
	 * <p><b>DO NOT MANUALLY USE THIS. IT CAN LEAD TO UNKNOWN BEHAVIOUR IN EXTENSIONS</b></p>
	 * TODO Delete this before the pr is actually merged and make {@link #get(Class)} throw. It was fun though
	 * @see #register(Class, String)
	 * 
	 * @param <R> The type of the {@link Value} class we are analyzing
	 * @param valueClass The class of the {@link Value}
	 */ //TODO Remove this. It was cool that it worked, nice entertainment, but it has to go.
	/*public static <R extends Value> void autoRegister(Class<R> valueClass) {
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
			CarpetSettings.LOG.warn("Couldn't automagically get type name for '"+valueClass.getName()+"', using class without 'value'. Please register it!");
		}
	}*/
}
