package carpet.script.value;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.Fluff;
import carpet.script.LazyValue;
import carpet.script.Tokenizer;
import carpet.script.bundled.Module;
import carpet.script.exception.BreakStatement;
import carpet.script.exception.ContinueStatement;
import carpet.script.exception.ExpressionException;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.ReturnStatement;
import carpet.script.exception.ThrowStatement;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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

    public Module getModule() {return expression.module;}

    @Override
    public String getPrettyString()
    {
        List<String> stringArgs= new ArrayList<>(args);
        if (outerState != null)
            stringArgs.addAll(outerState.entrySet().stream().map(e ->
                    "outer("+e.getKey()+") = "+e.getValue().evalValue(null).getPrettyString()).collect(Collectors.toList()));
        return (name.equals("_")?"<lambda>":name) +"("+String.join(", ",stringArgs)+")";
    }

    public String fullName() {return (name.equals("_")?"<lambda>":name)+(expression.module == null?"":"["+expression.module.getName()+"]");}

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
        return name.hashCode()+(int)variant;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof FunctionValue)
            return name.equals(((FunctionValue) o).name) && variant == ((FunctionValue) o).variant;
        return false;
    }

    @Override
    public int compareTo(final Value o)
    {
        if (o instanceof FunctionValue)
        {
            int nameSame = this.name.compareTo(((FunctionValue) o).name);
            if (nameSame != 0)
                return nameSame;
            return (int) (variant-((FunctionValue) o).variant);
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

    public LazyValue callInContext(Context c, Integer type, List<LazyValue> lazyParams)
    {
        try
        {
            return lazyEval(c, type, expression, token, lazyParams);
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
            throw new ExpressionException(c, expression, token, "Your math is wrong, "+exc.getMessage(), Collections.singletonList(this));
        }
    }

    @Override
    public LazyValue lazyEval(Context c, Integer type, Expression e, Tokenizer.Token t, List<LazyValue> lazyParams)
    {
        assertArgsOk(lazyParams, (fixedArgs) ->{
            if (fixedArgs)  // wrong number of args for fixed args
            {
                throw new ExpressionException(c, e, t,
                        "Incorrect number of arguments for function "+name+
                                ". Should be "+args.size()+", not "+lazyParams.size()+" like "+args
                );
            }
            else  // too few args for varargs
            {
                List<String> argList = new ArrayList<>(args);
                argList.add("... "+varArgs);
                throw new ExpressionException(c, e, t,
                        "Incorrect number of arguments for function "+name+
                                ". Should be at least "+args.size()+", not "+lazyParams.size()+" like "+argList
                );
            }
        });
        Context newFrame = c.recreate();

        if (outerState != null) outerState.forEach(newFrame::setVariable);
        for (int i=0; i<args.size(); i++)
        {
            String arg = args.get(i);
            Value val = lazyParams.get(i).evalValue(c).reboundedTo(arg); // todo check if we need to copy that
            newFrame.setVariable(arg, (cc, tt) -> val);
        }
        if (varArgs != null)
        {
            List<Value> extraParams = new ArrayList<>();
            for (int i = args.size(), mx = lazyParams.size(); i < mx; i++)
            {
                extraParams.add(lazyParams.get(i).evalValue(c).reboundedTo(null)); // copy by value I guess
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
    public String getVarArgs() {return varArgs; }

    @Override
    public Tag toTag(boolean force)
    {
        if (!force) throw new NBTSerializableValue.IncompatibleTypeException(this);
        return StringTag.of(getString());
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
    public static List<Value> resolveArgs(List<LazyValue> lzargs, Context c, Integer t)
    {
        List<Value> args = new ArrayList<>(lzargs.size());
        lzargs.forEach( v -> args.add( v.evalValue(c, t)));
        return args;
    }

    public static List<LazyValue> lazify(List<Value> args)
    {
        List<LazyValue> lzargs = new ArrayList<>(args.size());
        args.forEach( v -> lzargs.add( (c, t) -> v));
        return lzargs;
    }
}
