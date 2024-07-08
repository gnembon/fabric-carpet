package carpet.script.value;

import carpet.script.CarpetScriptServer;
import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.Fluff;
import carpet.script.LazyValue;
import carpet.script.Module;
import carpet.script.Tokenizer;
import carpet.script.exception.BreakStatement;
import carpet.script.exception.ContinueStatement;
import carpet.script.exception.ExpressionException;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.ReturnStatement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;

public class FunctionValue extends Value implements Fluff.ILazyFunction
{
    private final Expression expression;
    private final Tokenizer.Token token;
    private final String name;
    private final LazyValue body;
    private Map<String, LazyValue> outerState;
    private final List<String> args;
    private final String varArgs;
    private static long variantCounter = 1;
    private long variant;

    private FunctionValue(Expression expression, Tokenizer.Token token, String name, LazyValue body, List<String> args, String varArgs)
    {
        this.expression = expression;
        this.token = token;
        this.name = name;
        this.body = body;
        this.args = args;
        this.varArgs = varArgs;
        this.outerState = null;
        variant = 0L;
    }

    public FunctionValue(Expression expression, Tokenizer.Token token, String name, LazyValue body, List<String> args, String varArgs, Map<String, LazyValue> outerState)
    {
        this.expression = expression;
        this.token = token;
        this.name = name;
        this.body = body;
        this.args = args;
        this.varArgs = varArgs;
        this.outerState = outerState;
        variant = variantCounter++;
    }

    @Override
    public String getString()
    {
        return name;
    }

    public Module getModule()
    {
        return expression.module;
    }

    @Override
    public String getPrettyString()
    {
        List<String> stringArgs = new ArrayList<>(args);
        if (outerState != null)
        {
            stringArgs.addAll(outerState.entrySet().stream().map(e ->
                    "outer(" + e.getKey() + ") = " + e.getValue().evalValue(null).getPrettyString()).toList());
        }
        return (name.equals("_") ? "<lambda>" : name) + "(" + String.join(", ", stringArgs) + ")";
    }

    public String fullName()
    {
        return (name.equals("_") ? "<lambda>" : name) + (expression.module == null ? "" : "[" + expression.module.name() + "]");
    }

    @Override
    public boolean getBoolean()
    {
        return true;
    }

    @Override
    protected Value clone()
    {
        FunctionValue ret = new FunctionValue(expression, token, name, body, args, varArgs);
        ret.outerState = this.outerState;
        ret.variant = this.variant;
        return ret;
    }

    @Override
    public int hashCode()
    {
        return name.hashCode() + (int) variant;
    }

    @Override
    public boolean equals(Object o)
    {
        return o instanceof FunctionValue fv && name.equals(fv.name) && variant == fv.variant;
    }

    @Override
    public int compareTo(Value o)
    {
        if (o instanceof FunctionValue fv)
        {
            int nameSame = this.name.compareTo(fv.name);
            return nameSame != 0 ? nameSame : (int) (variant - fv.variant);
        }
        return getString().compareTo(o.getString());
    }

    @Override
    public double readDoubleNumber()
    {
        return getNumParams();
    }

    @Override
    public String getTypeString()
    {
        return "function";
    }

    @Override
    public Value slice(long from, Long to)
    {
        throw new InternalExpressionException("Cannot slice a function");
    }

    @Override
    public int getNumParams()
    {
        return args.size();
    }

    @Override
    public boolean numParamsVaries()
    {
        return varArgs != null;
    }

    public LazyValue callInContext(Context c, Context.Type type, List<Value> params)
    {
        try
        {
            return execute(c, type, expression, token, params, null);
        }
        catch (ExpressionException exc)
        {
            exc.stack.add(this);
            throw exc;
        }
        catch (InternalExpressionException exc)
        {
            exc.stack.add(this);
            throw new ExpressionException(c, expression, token, exc.getMessage(), exc.stack);
        }
        catch (ArithmeticException exc)
        {
            throw new ExpressionException(c, expression, token, "Your math is wrong, " + exc.getMessage(), Collections.singletonList(this));
        }
    }

    public void checkArgs(int candidates)
    {
        int actual = getArguments().size();

        if (candidates < actual)
        {
            throw new InternalExpressionException("Function " + getPrettyString() + " requires at least " + actual + " arguments");
        }
        if (candidates > actual && getVarArgs() == null)
        {
            throw new InternalExpressionException("Function " + getPrettyString() + " requires " + actual + " arguments");
        }
    }

    public static List<Value> unpackArgs(List<LazyValue> lazyParams, Context c)
    {
        // TODO we shoudn't need that if all fuctions are not lazy really
        List<Value> params = new ArrayList<>();
        for (LazyValue lv : lazyParams)
        {
            Value param = lv.evalValue(c, Context.NONE);
            if (param instanceof FunctionUnpackedArgumentsValue)
            {
                CarpetScriptServer.LOG.error("How did we get here?");
                params.addAll(((ListValue) param).getItems());
            }
            else
            {
                params.add(param);
            }
        }
        return params;
    }

    @Override
    public LazyValue lazyEval(Context c, Context.Type type, Expression e, Tokenizer.Token t, List<LazyValue> lazyParams)
    {
        List<Value> resolvedParams = unpackArgs(lazyParams, c);
        return execute(c, type, e, t, resolvedParams, null);
    }

    public LazyValue execute(Context c, Context.Type type, Expression e, Tokenizer.Token t, List<Value> params, @Nullable ThreadValue freshNewCallingThread)
    {
        assertArgsOk(params, fixedArgs -> {
            if (fixedArgs)  // wrong number of args for fixed args
            {
                throw new ExpressionException(c, e, t,
                        "Incorrect number of arguments for function " + name +
                                ". Should be " + args.size() + ", not " + params.size() + " like " + args
                );
            }
            // too few args for varargs

            List<String> argList = new ArrayList<>(args);
            argList.add("... " + varArgs);
            throw new ExpressionException(c, e, t,
                    "Incorrect number of arguments for function " + name +
                            ". Should be at least " + args.size() + ", not " + params.size() + " like " + argList
            );
        });
        Context newFrame = c.recreate();
        if (freshNewCallingThread != null)
        {
            newFrame.setThreadContext(freshNewCallingThread);
        }

        if (outerState != null)
        {
            outerState.forEach(newFrame::setVariable);
        }
        for (int i = 0; i < args.size(); i++)
        {
            String arg = args.get(i);
            Value val = params.get(i).reboundedTo(arg); // todo check if we need to copy that
            newFrame.setVariable(arg, (cc, tt) -> val);
        }
        if (varArgs != null)
        {
            List<Value> extraParams = new ArrayList<>();
            for (int i = args.size(), mx = params.size(); i < mx; i++)
            {
                extraParams.add(params.get(i).reboundedTo(null)); // copy by value I guess
            }
            Value rest = ListValue.wrap(extraParams).bindTo(varArgs); // didn't we just copied that?
            newFrame.setVariable(varArgs, (cc, tt) -> rest);

        }
        Value retVal;
        try
        {
            retVal = body.evalValue(newFrame, type); // todo not sure if we need to propagete type / consider boolean context in defined functions - answer seems ye
        }
        catch (BreakStatement | ContinueStatement exc)
        {
            throw new ExpressionException(c, e, t, "'continue' and 'break' can only be called inside loop function bodies");
        }
        catch (ReturnStatement returnStatement)
        {
            retVal = returnStatement.retval;
        }
        Value otherRetVal = retVal;
        return (cc, tt) -> otherRetVal;
    }

    public Expression getExpression()
    {
        return expression;
    }

    public Tokenizer.Token getToken()
    {
        return token;
    }

    public List<String> getArguments()
    {
        return args;
    }

    public String getVarArgs()
    {
        return varArgs;
    }

    @Override
    public Tag toTag(boolean force, RegistryAccess regs)
    {
        if (!force)
        {
            throw new NBTSerializableValue.IncompatibleTypeException(this);
        }
        return StringTag.valueOf(getString());
    }

    public void assertArgsOk(List<?> list, Consumer<Boolean> feedback)
    {
        int size = list.size();
        if (varArgs == null && args.size() != size) // wrong number of args for fixed args
        {
            feedback.accept(true);
        }
        else if (varArgs != null && args.size() > size) // too few args for varargs
        {
            feedback.accept(false);
        }
    }

    @Override
    public boolean pure()
    {
        return false;
    }

    @Override
    public boolean transitive()
    {
        return false;
    }
}
