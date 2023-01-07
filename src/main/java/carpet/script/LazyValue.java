package carpet.script;

import carpet.script.Context.Type;
import carpet.script.exception.InternalExpressionException;
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
            throw new InternalExpressionException("Outer scoping of variables is only possible in function signatures");
        }
    }
    
    public record VarArgsOrUnpacker(String name, LazyValue executable) implements Named {
        @Override
        public Value evalValue(Context c, Type type) {
            // This is an unpacker, just that it's referencing a variable! Just run the executable the operator gave us
            return executable.evalValue(c, type);
        }
    }
    
    public record Variable(String name, Expression expr) implements Named, Assignable {
        @Override
        public Value evalValue(Context c, Type type) {
            return expr.getAnyVariable(c, name);
        }

        @Override
        public void set(Context c, Value v) {
            expr.setAnyVariable(c, name, v);
        }
    }
    
    public record VarCall(LazyValue nameGetter, Expression expr) implements Assignable {
        @Override
        public Value evalValue(Context c, Type type) {
            return expr.getAnyVariable(c, nameGetter.evalValue(c, type).getString());
        }

        @Override
        public void set(Context c, Value v) {
            String name = nameGetter.evalValue(c).getString();
            expr.setAnyVariable(c, name, v);
        }
    }
    public sealed interface Assignable extends LazyValue permits Variable, VarCall {
        /**
         * Assigns this to the given {@link Value} in the specified {@link Context}
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
