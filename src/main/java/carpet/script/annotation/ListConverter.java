package carpet.script.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import carpet.CarpetSettings;
import carpet.script.value.ListValue;
import carpet.script.value.Value;

public class ListConverter<T> implements ValueConverter<List<T>> {

	private final ValueConverter<T> itemConverter;
	private final boolean allowSingletonCreation;
	
	@Override
	public String getTypeName() {
		return "list of " + itemConverter.getTypeName();
	}

	@Override
	public List<T> convert(Value value) { //TODO Actual checks. Singleton maker should return null if not instance, list value should not allow things
		return value instanceof ListValue 
				? ((ListValue)value).getItems().stream().map(itemConverter::convert).collect(Collectors.toList())
				: allowSingletonCreation ? Collections.singletonList(itemConverter.convert(value)) : null;
	}
	
	private ListConverter(Class<T> itemType, boolean allowSingletonCreation) {
		itemConverter = ValueConverter.fromType(itemType);
		this.allowSingletonCreation = allowSingletonCreation;
	}
	
	public static ListConverter<?> fromParameter(Parameter param) { //TODO Actual type-safety (or at least kinda)
		ParameterizedType type = (ParameterizedType) param.getParameterizedType();
		AnnotatedParameterizedType annotatedType = (AnnotatedParameterizedType)param.getAnnotatedType();
		//TODO (AnnotatedParameterizedType) via @Target(TYPE_USE)? Use annotations (+ nested annotations??)
		boolean allowSingletonCreation = param.isAnnotationPresent(AllowSingleton.class);
		return new ListConverter<>((Class<?>)type.getActualTypeArguments()[0], allowSingletonCreation);
	}

}
