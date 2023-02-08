package carpet.script;

import carpet.script.value.Value;

/**
 * LazyNumber interface created for lazily evaluated functions
 */
@FunctionalInterface
public interface LazyValue
{
    LazyValue FALSE = (c, t) -> Value.FALSE;
    LazyValue TRUE = (c, t) -> Value.TRUE;
    LazyValue NULL = (c, t) -> Value.NULL;
    LazyValue ZERO = (c, t) -> Value.ZERO;

    static LazyValue ofConstant(Value val)
    {
        return new Constant(val);
    }

    Value evalValue(Context c, Context.Type type);

    default Value evalValue(Context c)
    {
        return evalValue(c, Context.Type.NONE);
    }

    @FunctionalInterface
    interface ContextFreeLazyValue extends LazyValue
    {

        Value evalType(Context.Type type);

        @Override
        default Value evalValue(Context c, Context.Type type)
        {
            return evalType(type);
        }
    }


    class Constant implements ContextFreeLazyValue
    {
        Value result;

        public Constant(Value value)
        {
            result = value;
        }

        public Value get()
        {
            return result;
        }

        @Override
        public Value evalType(Context.Type type)
        {

            return result.fromConstant();
        }

        @Override
        public Value evalValue(Context c, Context.Type type)
        {
            return result.fromConstant();
        }
    }
}
