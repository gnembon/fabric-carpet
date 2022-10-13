package carpet.script;

import carpet.script.Context.Type;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.AbstractListValue;
import carpet.script.value.FunctionUnpackedArgumentsValue;
import carpet.script.value.Value;

/** LazyNumber interface created for lazily evaluated functions */
@FunctionalInterface
public interface LazyValue
{
    LazyValue FALSE = (c, t) -> Value.FALSE;
    LazyValue TRUE = (c, t) -> Value.TRUE;
    LazyValue NULL = (c, t) -> Value.NULL;
    LazyValue ZERO = (c, t) -> Value.ZERO;

    public static LazyValue ofConstant(Value val) {
        return new Constant(val);
    }

    Value evalValue(Context c, Context.Type type);

    default Value evalValue(Context c){
        return evalValue(c, Context.Type.NONE);
    }

    @FunctionalInterface
    interface ContextFreeLazyValue extends LazyValue
    {
        Value evalType(Context.Type type);

        @Override
        default Value evalValue(Context c, Context.Type type) {
            return evalType(type);
        }
    }
    
    public sealed interface Named extends LazyValue permits Variable, Outer, VarArgsOrUnpacker {
    	String name();
    }
    
    public record Outer(String name) implements Named {
    	@Override
    	public Value evalValue(Context c, Type type) {
    		throw new InternalExpressionException("'outer' can only be used in a function signature");
    	}
    }
    
    public record VarArgsOrUnpacker(String name, LazyValue param) implements Named {
    	@Override
    	public Value evalValue(Context c, Type type) {
    		// this can only be called in an unpacking context, varargs would just call name()
    		assert type != Type.LOCALIZATION;
    		// TODO I dislike this being here
    		Value params = param.evalValue(c, type);
            if (!(params instanceof AbstractListValue))
                throw new InternalExpressionException("Unable to unpack a non-list");
            return new FunctionUnpackedArgumentsValue( ((AbstractListValue) params).unpack());
    	}
    }
    
    public record Variable(String name, Expression expr) implements Named, Assignable {
		@Override
		public Value evalValue(Context c, Type type) {
			return expr.getOrSetAnyVariable(c, name).evalValue(c, type);
		}
		
		@Override
		public void set(Context c, Value v) {
			v.bindTo(name);
			expr.setAnyVariable(c, name, (cc, tt) -> v);
		}
    }
    
    public record VarCall(LazyValue nameGetter, Expression expr) implements Assignable {
    	@Override
    	public Value evalValue(Context c, Type type) {
    		return expr.getOrSetAnyVariable(c, nameGetter.evalValue(c, type).getString()).evalValue(c);
    	}
    	
    	@Override
		public void set(Context c, Value v) {
    		String name = nameGetter.evalValue(c).getString();
    		v.bindTo(name);
    		expr.setAnyVariable(c, name, (cc, tt) -> v);
    	}
    }
    public sealed interface Assignable extends LazyValue permits Variable, VarCall {
    	/**
    	 * Assigns this to the given {@link Value} in the specified {@link Context}
    	 * 
    	 * @implNote This method <b>MUST</b> bind the variable to its new name, though it will
    	 * 			 <b>NOT</b> make a copy
    	 * 
    	 * @param c The {@link Context} that may be used if the variable is a local
    	 * @param v The value to set this assignable to
    	 */
    	void set(Context c, Value v);
    }


    public record Constant(Value value) implements ContextFreeLazyValue
    {
        @Override
        public Value evalType(Context.Type type) {
            return value.fromConstant();
        }
    }
}
