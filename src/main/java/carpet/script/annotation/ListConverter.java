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
		return (allowSingletonCreation ? itemConverter.getTypeName() + " or ": "") + "a list of " + itemConverter.getTypeName() + "s"; //TODO Decide if this good
	}

	@Override
	public List<T> convert(Value value) { //TODO Actual checks. Singleton maker should return null if not instance, list value should not allow things
		return value instanceof ListValue 
				? ((ListValue)value).getItems().stream().map(itemConverter::convert).collect(Collectors.toList())
				: allowSingletonCreation ? Collections.singletonList(itemConverter.convert(value)) : null;
	}
	
	private ListConverter(AnnotatedType itemType, boolean allowSingletonCreation) {
		itemConverter = ValueConverter.fromAnnotatedType(itemType);
		this.allowSingletonCreation = allowSingletonCreation;
	}
	
	public static ListConverter<?> fromAnnotatedType(AnnotatedType annotatedType) { //TODO Check actual type-safety (or at least kinda)
		AnnotatedParameterizedType paramType = (AnnotatedParameterizedType) annotatedType;
		AnnotatedType itemType = paramType.getAnnotatedActualTypeArguments()[0];
		boolean allowSingletonCreation = annotatedType.isAnnotationPresent(AllowSingleton.class);
		return new ListConverter<>(itemType, allowSingletonCreation);
	}

}
