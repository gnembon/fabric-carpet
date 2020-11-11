package carpet.script.argument;

import carpet.script.Context;
import carpet.script.LazyValue;
import carpet.script.bundled.Module;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.FunctionValue;
import carpet.script.value.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FunctionArgument extends Argument
{
    public FunctionValue function;
    public List<LazyValue> args;

    private FunctionArgument(FunctionValue function, int offset, List<LazyValue> args)
    {
        super(offset);
        this.function = function;
        this.args = args;
    }

    /**
     * @param c context
     * @param module module
     * @param params list of lazy params
     * @param offset offset where to start looking for functional argument
     * @param allowNone none indicates no function present, otherwise it will croak
     * @param checkArgs whether the caller expects trailing parameters to fully resolve function argument list
     *                  if not - argument count check will not be performed and its up to the caller to verify
     *                  if the number of supplied arguments is right
     * @return argument data
     */
    public static FunctionArgument findIn(
            Context c,
            Module module,
            List<LazyValue> params,
            int offset,
            boolean allowNone,
            boolean checkArgs)
    {
        Value functionValue = params.get(offset).evalValue(c);
        if (functionValue.isNull())
        {
            if (allowNone) return new FunctionArgument(null, offset+1, Collections.emptyList());
            throw new InternalExpressionException("function argument cannot be null");
        }
        if (!(functionValue instanceof FunctionValue))
        {
            String name = functionValue.getString();
            functionValue = c.host.getAssertFunction(module, name);
        }
        FunctionValue fun = (FunctionValue)functionValue;
        int argsize = fun.getArguments().size();
        if (checkArgs)
        {
            int extraargs = params.size() - argsize - offset - 1;
            if (extraargs < 0)
                throw new InternalExpressionException("Function " + fun.getPrettyString() + " requires at least " + fun.getArguments().size() + " arguments");
            if (extraargs > 0 && fun.getVarArgs() == null)
                throw new InternalExpressionException("Function " + fun.getPrettyString() + " requires " + fun.getArguments().size() + " arguments");
        }
        List<LazyValue> lvargs = new ArrayList<>();
        for (int i = offset+1, mx = params.size(); i < mx; i++) lvargs.add(params.get(i));
        return new FunctionArgument(fun, offset+1+argsize, lvargs);
    }

    public List<Value> resolveArgs(Context context, Integer type)
    {
        List<Value> res = new ArrayList<>(args.size());
        for (LazyValue arg : args) res.add(arg.evalValue(context, type));
        return res;
    }
}
