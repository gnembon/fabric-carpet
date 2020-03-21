package carpet.script.value;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.Fluff;
import carpet.script.LazyValue;
import carpet.script.Tokenizer;
import carpet.script.exception.BreakStatement;
import carpet.script.exception.ContinueStatement;
import carpet.script.exception.ExitStatement;
import carpet.script.exception.ExpressionException;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.ReturnStatement;
import carpet.script.exception.ThrowStatement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FunctionValue extends Value implements Fluff.ILazyFunction
{
    private Expression expression;
    private Tokenizer.Token token;
    private String name;
    private LazyValue body;
    private Map<String, LazyValue> outerState;
    private List<String> args;
    private static long variantCounter = 1;
    private long variant;

    private FunctionValue(Expression expression, Tokenizer.Token token, String name, LazyValue body, List<String> args)
    {
        this.expression = expression;
        this.token = token;
        this.name = name;
        this.body = body;
        this.args = args;
        this.outerState = null;
        variant = 0L;
    }

    public FunctionValue(Expression expression, Tokenizer.Token token, String name, LazyValue body, List<String> args, Map<String, LazyValue> outerState)
    {
        this.expression = expression;
        this.token = token;
        this.name = name;
        this.body = body;
        this.args = args;
        this.outerState = outerState;
        variant = variantCounter++;
    }

    @Override
    public String getString()
    {
        return name;
    }

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
        FunctionValue ret = new FunctionValue(expression, token, name, body, args);
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
    public double readNumber()
    {
        throw new InternalExpressionException("Function value has no numeric value");
    }

    @Override
    public String getTypeString()
    {
        return "function";
    }

    @Override
    public Value slice(long from, long to)
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
        return false;
    }

    public LazyValue callInContext(Expression callingExpression, Context c, Integer type, Expression e, Tokenizer.Token t, List<LazyValue> lazyParams)
    {
        try
        {
            return lazyEval(c, type, e, t, lazyParams);
        }
        catch (ExpressionException exc)
        {
            exc.stack.add(this);
            throw exc;
        }
        catch (InternalExpressionException exc)
        {
            exc.stack.add(this);
            throw new ExpressionException(c, e, t, exc.getMessage(), exc.stack);
        }

        catch (ArithmeticException exc)
        {
            throw new ExpressionException(c, e, t, "Your math is wrong, "+exc.getMessage(), Collections.singletonList(this));
        }
    }

    @Override
    public LazyValue lazyEval(Context c, Integer type, Expression e, Tokenizer.Token t, List<LazyValue> lazyParams)
    {
        if (args.size() != lazyParams.size()) // something that might be subject to change in the future
                                              // in case we add variable arguments
        {
            throw new ExpressionException(c, e, t,
                    "Incorrect number of arguments for function "+name+
                            ". Should be "+args.size()+", not "+lazyParams.size()+" like "+args
            );
        }
        Context newFrame = c.recreate();

        if (outerState != null) outerState.forEach(newFrame::setVariable);
        for (int i=0; i<args.size(); i++)
        {
            String arg = args.get(i);
            Value val = lazyParams.get(i).evalValue(c).reboundedTo(arg);
            newFrame.setVariable(arg, (cc, tt) -> val);
        }
        Value retVal;
        boolean rethrow = false;
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
        catch (ThrowStatement throwStatement) // might not be really necessary
        {
            retVal = throwStatement.retval;
            rethrow = true;
        }
        //catch (ArithmeticException | ExitStatement | ExpressionException | InternalExpressionException exc )
        //{
        //    throw exc; // rethrow so could be contextualized if needed
        //}
        if (rethrow)
        {
            throw new ThrowStatement(retVal);
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
}
