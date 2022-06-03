package carpet.script;

import java.util.ArrayList;
import java.util.List;

import carpet.script.Context.Type;
import carpet.script.Expression.ExpressionNode;
import carpet.script.value.ListValue.ListConstructorValue;
import carpet.script.value.Value;

// TODO this better
public record ReferenceArray(String[] variables, Expression expression) implements LazyValue {
	// TODO assert this isn't called lazily, or we could be putting newer stuff into lists created before changes
	@Override
	public Value evalValue(Context c, Type type) {
	    List<Value> contents = new ArrayList<>(variables.length);
		for (int i = 0; i < variables.length; i++) {
			contents.add(getValue(c, type, i));
		}
		return new ListConstructorValue(contents);
	}
	
	private Value getValue(Context c, Context.Type type, int index) {
		return expression.getOrSetAnyVariable(c, variables[index]).evalValue(c, type);
	}
	
	public Value getValue(Context c, int index) {
		return getValue(c, Context.NONE, index);
	}
	
	public int size() {
		return variables.length;
	}
	
	static ReferenceArray of(ExpressionNode list, Expression e) {
		String[] values = new String[list.args.size()];
		int i = 0;
		for (ExpressionNode node : list.args) {
			values[i] = node.token.surface;
			i++;
		}
		return new ReferenceArray(values, e);
	}
}
