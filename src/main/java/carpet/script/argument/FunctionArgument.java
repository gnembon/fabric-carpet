package carpet.script.argument;

import carpet.script.Context;
import carpet.script.ScriptHost;
import carpet.script.Module;
import carpet.script.command.CommandArgument;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.FunctionValue;
import carpet.script.value.ListValue;
import carpet.script.value.Value;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FunctionArgument extends Argument
{
    public final FunctionValue function;
    public final List<Value> args;

    private FunctionArgument(@Nullable FunctionValue function, int offset, List<Value> args)
    {
        super(offset);
        this.function = function;
        this.args = args;
    }

    /**
     * @param c         context
     * @param module    module
     * @param params    list of params
     * @param offset    offset where to start looking for functional argument
     * @param allowNone none indicates no function present, otherwise it will croak
     * @param checkArgs whether the caller expects trailing parameters to fully resolve function argument list
     *                  if not - argument count check will not be performed and its up to the caller to verify
     *                  if the number of supplied arguments is right
     * @return argument data
     */
    public static FunctionArgument findIn(
            Context c,
            Module module,
            List<Value> params,
            int offset,
            boolean allowNone,
            boolean checkArgs)
    {
        Value functionValue = params.get(offset);
        if (functionValue.isNull())
        {
            if (allowNone)
            {
                return new FunctionArgument(null, offset + 1, Collections.emptyList());
            }
            throw new InternalExpressionException("function argument cannot be null");
        }
        if (!(functionValue instanceof FunctionValue))
        {
            String name = functionValue.getString();
            functionValue = c.host.getAssertFunction(module, name);
        }
        FunctionValue fun = (FunctionValue) functionValue;
        int argsize = fun.getArguments().size();
        if (checkArgs)
        {
            int extraargs = params.size() - argsize - offset - 1;
            if (extraargs < 0)
            {
                throw new InternalExpressionException("Function " + fun.getPrettyString() + " requires at least " + fun.getArguments().size() + " arguments");
            }
            if (extraargs > 0 && fun.getVarArgs() == null)
            {
                throw new InternalExpressionException("Function " + fun.getPrettyString() + " requires " + fun.getArguments().size() + " arguments");
            }
        }
        List<Value> lvargs = new ArrayList<>();
        for (int i = offset + 1, mx = params.size(); i < mx; i++)
        {
            lvargs.add(params.get(i));
        }
        return new FunctionArgument(fun, offset + 1 + argsize, lvargs);
    }

    public static FunctionArgument fromCommandSpec(ScriptHost host, Value funSpec) throws CommandSyntaxException
    {
        FunctionValue function;
        List<Value> args = Collections.emptyList();
        if (!(funSpec instanceof ListValue))
        {
            funSpec = ListValue.of(funSpec);
        }
        List<Value> params = ((ListValue) funSpec).getItems();
        if (params.isEmpty())
        {
            throw CommandArgument.error("Function has empty spec");
        }
        Value first = params.get(0);
        if (first instanceof FunctionValue)
        {
            function = (FunctionValue) first;
        }
        else
        {
            String name = first.getString();
            function = host.getFunction(name);
            if (function == null)
            {
                throw CommandArgument.error("Function " + name + " is not defined yet");
            }
        }
        if (params.size() > 1)
        {
            args = params.subList(1, params.size());
        }
        return new FunctionArgument(function, 0, args);
    }

    public List<Value> checkedArgs()
    {
        function.checkArgs(args.size());
        return args;
    }
}
