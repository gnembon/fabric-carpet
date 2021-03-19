package carpet.script.annotation;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import carpet.script.value.ListValue;
import carpet.script.value.Value;

public class ListConverter<T> implements ValueConverter<List<T>> {
	private final ValueConverter<T> itemConverter;
	private final boolean allowSingletonCreation;
	
	@Override
	public String getTypeName() {
		return (allowSingletonCreation ? itemConverter.getTypeName() + " or ": "") + "a list of " + itemConverter.getTypeName() + "s";//TODO Decide if this good
	}

	@Override
	public List<T> convert(Value value) { //TODO Actual checks. Singleton maker should return null if not instance, list value should not allow other things
		return value instanceof ListValue 
				? ((ListValue)value).getItems().stream().map(itemConverter::convert).collect(Collectors.toList())
				: allowSingletonCreation ? Collections.singletonList(itemConverter.convert(value)) : null;
	}
	
	private ListConverter(AnnotatedType itemType, boolean allowSingletonCreation) {
		itemConverter = ValueConverter.fromAnnotatedType(itemType);
		this.allowSingletonCreation = allowSingletonCreation;
	}
	
	/**
	 * <p>Returns a new {@link ListConverter} to convert to the given {@link AnnotatedType}.</p>
	 * 
	 * <p>The given {@link ValueConverter} will convert the objects inside the list to the
	 * generics specified in the {@link AnnotatedType}, and the {@link ValueConverter} will
	 * be set to accept non-list (but correct) items and make a singleton out of them     //TODO "Correct"
	 * if the {@link AllowSingleton} annotation has been specified.</p>
	 * 
	 * @apiNote This method expects the {@link AnnotatedType} to already be of {@link List} type, and, while it will
	 *          technically accept a non-{@link List} {@link AnnotatedType}, it will fail if it doesn't has at least
	 *          a generic parameter with an {@link ArrayIndexOutOfBoundsException}. 
	 * @param annotatedType The type to get generics information from
	 * @return A new {@link ListConverter} for the data specified in the {@link AnnotatedType}
	 */
	public static ListConverter<?> fromAnnotatedType(AnnotatedType annotatedType) { //TODO Assert actual type-safety (or at least kinda)
		AnnotatedParameterizedType paramType = (AnnotatedParameterizedType) annotatedType;
		AnnotatedType itemType = paramType.getAnnotatedActualTypeArguments()[0];
		boolean allowSingletonCreation = annotatedType.isAnnotationPresent(AllowSingleton.class);
		return new ListConverter<>(itemType, allowSingletonCreation);
	}

}
