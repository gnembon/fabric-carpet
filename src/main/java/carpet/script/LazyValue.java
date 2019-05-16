package carpet.script;

import carpet.script.value.Value;

/** LazyNumber interface created for lazily evaluated functions */
@FunctionalInterface
public interface LazyValue
{
    LazyValue FALSE = (c, t) -> Value.FALSE;
    LazyValue TRUE = (c, t) -> Value.TRUE;
    LazyValue NULL = (c, t) -> Value.NULL;
    LazyValue ZERO = (c, t) -> Value.ZERO;
    /**
     * The Value representation of the left parenthesis, used for parsing
     * varying numbers of function parameters.
     */
    LazyValue PARAMS_START = (c, t) -> null;

    Value evalValue(Context c, Integer type);

    default Value evalValue(Context c){
        return evalValue(c, 0);
    }
}
