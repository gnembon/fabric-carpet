package carpet.script.value;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.ExpressionInspector;
import carpet.script.Fluff;
import carpet.script.LazyValue;
import carpet.script.Tokenizer;
import carpet.script.exception.ExitStatement;
import carpet.script.exception.ExpressionException;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.ReturnStatement;
import carpet.script.exception.ThrowStatement;

import java.util.List;
import java.util.Map;

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

    public FunctionValue(Expression expression, Tokenizer.Token token, String name, LazyValue body, List<String> args)
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
        // this hole thing might not be really needed
        Expression callContext = ExpressionInspector.Expression_cloneWithName(callingExpression, name, t);
        try
        {
            return lazyEval(c, type, e, t, lazyParams);
        }
        catch (InternalExpressionException exc)
        {
            throw new ExpressionException(callContext, t, exc.getMessage());
        }
        catch (ArithmeticException exc)
        {
            throw new ExpressionException(callContext, t, "Your math is wrong, "+exc.getMessage());
        }
    }

    @Override
    public LazyValue lazyEval(Context c, Integer type, Expression e, Tokenizer.Token t, List<LazyValue> lazyParams)
    {
        if (args.size() != lazyParams.size()) // something that might be subject to change in the future
                                              // in case we add variable arguments
        {
            throw new ExpressionException(e, t,
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
        catch (ReturnStatement returnStatement)
        {
            retVal = returnStatement.retval;
        }
        catch (ThrowStatement throwStatement) // might not be really necessary
        {
            retVal = throwStatement.retval;
            rethrow = true;
        }
        catch (ArithmeticException | ExitStatement | ExpressionException exc)
        {
            throw exc; // rethrow so could be contextualized if needed
        }
        catch (Exception exc)
        {
            throw new ExpressionException(e, t, "Error while evaluating expression: "+exc.getMessage());
        }
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
