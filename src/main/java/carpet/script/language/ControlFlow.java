package carpet.script.language;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.LazyValue;
import carpet.script.exception.ExitStatement;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.ProcessedThrowStatement;
import carpet.script.exception.ThrowStatement;
import carpet.script.value.StringValue;
import carpet.script.value.Value;

public class ControlFlow {
    public static void apply(Expression expression) // public just to get the javadoc right
    {
        expression.addLazyBinaryOperator(";", Operators.precedence.get("nextop;"), true, (c, t, lv1, lv2) ->
        {
            lv1.evalValue(c, Context.VOID);
            Value v2 = lv2.evalValue(c, t);
            return (cc, tt) -> v2;
        });

        // if(cond1, expr1, cond2, expr2, ..., ?default) => value
        expression.addLazyFunction("if", -1, (c, t, lv) ->
        {
            if ( lv.size() < 2 )
                throw new InternalExpressionException("'if' statement needs to have at least one condition and one case");
            for (int i=0; i<lv.size()-1; i+=2)
            {
                if (lv.get(i).evalValue(c, Context.BOOLEAN).getBoolean())
                {
                    //int iFinal = i;
                    Value ret = lv.get(i+1).evalValue(c, t);
                    return (cc, tt) -> ret;
                }
            }
            if (lv.size()%2 == 1)
            {
                Value ret = lv.get(lv.size() - 1).evalValue(c, t);
                return (cc, tt) -> ret;
            }
            return (cc, tt) -> Value.NULL;
        });

        expression.addFunction("exit", (lv) -> { throw new ExitStatement(lv.size()==0?Value.NULL:lv.get(0)); });

        expression.addLazyFunction("throw", -1, (c, t, lv)-> 
        {
            switch (lv.size()) 
            {
                case 0:
                    throw new ThrowStatement(new StringValue("No further information"), Value.NULL, Value.NULL);
                case 1:
                    throw new ThrowStatement(lv.get(0).evalValue(c));
                case 2:
                    throw new ThrowStatement(lv.get(1).evalValue(c), lv.get(0).evalValue(c), Value.NULL);
                case 3:
                    throw new ThrowStatement(lv.get(1).evalValue(c), lv.get(0).evalValue(c), lv.get(2).evalValue(c));
                default:
                    throw new InternalExpressionException("throw() can't accept more than 3 parameters");
            }
        });

        expression.addLazyFunction("try", -1, (c, t, lv) ->
        {
            if (lv.size()==0)
                throw new InternalExpressionException("'try' needs at least an expression block");
            try
            {
                Value retval = lv.get(0).evalValue(c, t);
                return (c_, t_) -> retval;
            }
            catch (ProcessedThrowStatement ret)
            {
                if (ret.exception.isError())
                    throw ret;
                if (lv.size() == 1)
                    return (c_, t_) -> Value.NULL;
                LazyValue __ = c.getVariable("_");
                c.setVariable("_", (__c, __t) -> ret.exception.getValue().reboundedTo("_"));
                LazyValue __msg = c.getVariable("_msg");
                c.setVariable("_msg", (__c, __t) -> StringValue.of(ret.message).reboundedTo("_msg"));
                Value val;
                if (lv.size() == 3)
                {
                    if (!ret.exception.isInstance(lv.get(1).evalValue(c)))
                    {
                        c.setVariable("_", __);
                        c.setVariable("_msg", __msg);
                        throw ret;
                    }
                    val = lv.get(2).evalValue(c, t);
                }
                else
                {
                    val = lv.get(1).evalValue(c, t);
                }
                c.setVariable("_",__);
                c.setVariable("_msg", __msg);
                return (c_, t_) -> val;
            }
        });
    }
}
