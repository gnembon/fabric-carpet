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

import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

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

    private FunctionValue(final Expression expression, final Tokenizer.Token token, final String name, final LazyValue body, final List<String> args, final String varArgs)
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

    public FunctionValue(final Expression expression, final Tokenizer.Token token, final String name, final LazyValue body, final List<String> args, final String varArgs, final Map<String, LazyValue> outerState)
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
        final List<String> stringArgs = new ArrayList<>(args);
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
        final FunctionValue ret = new FunctionValue(expression, token, name, body, args, varArgs);
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
    public boolean equals(final Object o)
    {
        return o instanceof final FunctionValue fv && name.equals(fv.name) && variant == fv.variant;
    }

    @Override
    public int compareTo(final Value o)
    {
        if (o instanceof final FunctionValue fv)
        {
            final int nameSame = this.name.compareTo(fv.name);
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
    public Value slice(final long from, final Long to)
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

    public LazyValue callInContext(final Context c, final Context.Type type, final List<Value> params)
    {
        try
        {
            return execute(c, type, expression, token, params);
        }
        catch (final ExpressionException exc)
        {
            exc.stack.add(this);
            throw exc;
        }
        catch (final InternalExpressionException exc)
        {
            exc.stack.add(this);
            throw new ExpressionException(c, expression, token, exc.getMessage(), exc.stack);
        }
        catch (final ArithmeticException exc)
        {
            throw new ExpressionException(c, expression, token, "Your math is wrong, " + exc.getMessage(), Collections.singletonList(this));
        }
    }

    public void checkArgs(final int candidates)
    {
        final int actual = getArguments().size();

        if (candidates < actual)
        {
            throw new InternalExpressionException("Function " + getPrettyString() + " requires at least " + actual + " arguments");
        }
        if (candidates > actual && getVarArgs() == null)
        {
            throw new InternalExpressionException("Function " + getPrettyString() + " requires " + actual + " arguments");
        }
    }

    public static List<Value> unpackArgs(final List<LazyValue> lazyParams, final Context c)
    {
        // TODO we shoudn't need that if all fuctions are not lazy really
        final List<Value> params = new ArrayList<>();
        for (final LazyValue lv : lazyParams)
        {
            final Value param = lv.evalValue(c, Context.NONE);
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
    public LazyValue lazyEval(final Context c, final Context.Type type, final Expression e, final Tokenizer.Token t, final List<LazyValue> lazyParams)
    {
        final List<Value> resolvedParams = unpackArgs(lazyParams, c);
        return execute(c, type, e, t, resolvedParams);
    }

    public LazyValue execute(final Context c, final Context.Type type, final Expression e, final Tokenizer.Token t, final List<Value> params)
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

            final List<String> argList = new ArrayList<>(args);
            argList.add("... " + varArgs);
            throw new ExpressionException(c, e, t,
                    "Incorrect number of arguments for function " + name +
                            ". Should be at least " + args.size() + ", not " + params.size() + " like " + argList
            );
        });
        final Context newFrame = c.recreate();

        if (outerState != null)
        {
            outerState.forEach(newFrame::setVariable);
        }
        for (int i = 0; i < args.size(); i++)
        {
            final String arg = args.get(i);
            final Value val = params.get(i).reboundedTo(arg); // todo check if we need to copy that
            newFrame.setVariable(arg, (cc, tt) -> val);
        }
        if (varArgs != null)
        {
            final List<Value> extraParams = new ArrayList<>();
            for (int i = args.size(), mx = params.size(); i < mx; i++)
            {
                extraParams.add(params.get(i).reboundedTo(null)); // copy by value I guess
            }
            final Value rest = ListValue.wrap(extraParams).bindTo(varArgs); // didn't we just copied that?
            newFrame.setVariable(varArgs, (cc, tt) -> rest);

        }
        Value retVal;
        try
        {
            retVal = body.evalValue(newFrame, type); // todo not sure if we need to propagete type / consider boolean context in defined functions - answer seems ye
        }
        catch (final BreakStatement | ContinueStatement exc)
        {
            throw new ExpressionException(c, e, t, "'continue' and 'break' can only be called inside loop function bodies");
        }
        catch (final ReturnStatement returnStatement)
        {
            retVal = returnStatement.retval;
        }
        final Value otherRetVal = retVal;
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
    public Tag toTag(final boolean force)
    {
        if (!force)
        {
            throw new NBTSerializableValue.IncompatibleTypeException(this);
        }
        return StringTag.valueOf(getString());
    }

    public void assertArgsOk(final List<?> list, final Consumer<Boolean> feedback)
    {
        final int size = list.size();
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
