package carpet.script;

import java.util.ArrayList;
import java.util.List;

import carpet.script.Context.Type;
import carpet.script.value.ListValue;
import carpet.script.value.Value;

public record ReferenceArray(String[] variables, Expression expression) implements LazyValue {
	@Override
	public Value evalValue(Context c, Type type) {
	    List<Value> contents = new ArrayList<>(variables.length);
		for (int i = 0; i < variables.length; i++) {
			contents.add(getValue(c, i));
		}
		return ListValue.wrap(contents);
	}

	/**
	 * Gets the value at the given index without having to instantiate all about the list, for use in
	 * the {@code +=} and {@code <>} operators
	 */
	public Value getValue(Context c, int index) {
		return expression.getAnyVariable(c, variables[index]);
	}
	
	public int size() {
		return variables.length;
	}
}
