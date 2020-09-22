package carpet.script.argument;

import carpet.script.Context;
import carpet.script.LazyValue;
import carpet.script.bundled.Module;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.FunctionValue;
import carpet.script.value.Value;

import java.util.ArrayList;
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
    public static FunctionArgument findIn(Context c, Module module, List<LazyValue> params, int offset, boolean prematureEvaluation)
    {
        Value functionValue = params.get(offset).evalValue(c);
        if (!(functionValue instanceof FunctionValue))
        {
            String name = functionValue.getString();
            functionValue = c.host.getAssertFunction(module, name);
        }
        FunctionValue fun = (FunctionValue)functionValue;
        int argsize = fun.getArguments().size();
        int extraargs = params.size() - argsize - offset - 1;
        if (extraargs < 0)
            throw new InternalExpressionException("Function "+fun.getPrettyString()+" requires at least "+fun.getArguments().size()+" arguments");
        if (extraargs > 0 && fun.getVarArgs()==null)
            throw new InternalExpressionException("Function "+fun.getPrettyString()+" requires "+fun.getArguments().size()+" arguments");
        List<LazyValue> lvargs = new ArrayList<>();
        if (prematureEvaluation)
        {
            for (int i = offset+1, mx = params.size(); i < mx; i++)
            {
                Value arg = params.get(i).evalValue(c);
                lvargs.add((cc, tt) -> arg);
            }
        }
        else
        {
            for (int i = offset+1, mx = params.size(); i < mx; i++) lvargs.add(params.get(i));
        }
        return new FunctionArgument(fun, offset+1+argsize, lvargs);
    }

    public List<Value> resolveArgs(Context context, Integer type)
    {
        List<Value> res = new ArrayList<>(args.size());
        for (LazyValue arg : args) res.add(arg.evalValue(context, type));
        return res;
    }
}
