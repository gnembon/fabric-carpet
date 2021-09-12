package carpet.utils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.apache.commons.lang3.ClassUtils;

/**
 * <p>A wrapper around a {@link Field} that uses a {@link VarHandle} and {@link MethodHandle}s to retrieve and set the contents of the field</p>
 * 
 * <p>It keeps the memory semantics of {@code volatile} if the field had that modifier.</p><!-- TODO is this needed? -->
 *
 * @param <T> The {@link Field}'s type
 */
public final class TypedField<T> {
	private final VarHandle handle;
	private final Class<T> returnType;
	private final boolean isVolatile;
	
	/**
	 * <p>Constructs a {@link TypedField} around the passed {@link Field}.</p>
	 * @param field The {@link Field} to be around
	 * @throws IllegalAccessException If the class doesn't have read/write access to the passed field
	 */
	@SuppressWarnings("unchecked") // The only place we extract T from is the field's type, and primitive and wrappers have the same generic type
	public TypedField(Field field) throws IllegalAccessException {
		this.returnType = (Class<T>) ClassUtils.primitiveToWrapper(field.getType());
		this.handle = MethodHandles.lookup().unreflectVarHandle(field);
		this.isVolatile = Modifier.isVolatile(field.getModifiers());
		// TODO Use MethodHandles.varHandleExactInvoker? Would remove the isVolatile boolean (would be at constr), but would force a try catch in get/set methods
	}
	
	/**
	 * <p>Gets the contents of the field, in case it's static.</p>
	 * 
	 * <p>Throws otherwise</p>
	 */
	public T getStatic() {
		if (isVolatile) {
			return (T) handle.getVolatile();
		}
		return (T) handle.get();
	}
	
	/**
	 * <p>Gets the contents of the field, in case it's bound to an instance, and the passed object is an instance of it.</p>
	 * 
	 * <p>Throws otherwise.</p>
	 * @param instance An instance of the class that contains this field
	 */
	public T get(Object instance) {
		if (isVolatile) {
			return (T) handle.getVolatile(instance);
		}
		return (T) handle.get(instance);
	}
	
	/**
	 * <p>Sets the contents of the field, in case it's static</p>
	 * 
	 * <p>Throws otherwise</p>
	 * 
	 * @param content The contents to put in the field
	 */
	public void setStatic(T content) {
		if (isVolatile) {
			handle.setVolatile(content);
		} else {
			handle.set(content);
		}
	}
	
	/**
	 * <p>Sets the contents of the field, in case it's bound to an instance, and the passed object is an instance of it.</p>
	 * 
	 * <p>Throws otherwise</p>
	 * 
	 * @param instance An instance of the class that contains this field
	 * @param content The contents to put in the instance's field
	 */
	public void set(Object instance, T content) {
		if (isVolatile) {
			handle.setVolatile(instance, content);
		} else {
			handle.set(instance, content);
		}
	}
	
	/**
	 * <p>Returns the wrapped type of the contents of this field.</p>
	 */
	public Class<T> type() {
		return returnType;
	}
}
