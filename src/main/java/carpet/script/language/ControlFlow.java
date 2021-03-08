package carpet.script.language;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.LazyValue;
import carpet.script.exception.ExitStatement;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.ProcessedThrowStatement;
import carpet.script.exception.ThrowStatement;
import carpet.script.exception.Throwables;
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
                    throw new ThrowStatement(Value.NULL, Throwables.USER_DEFINED);
                case 1:
                    throw new ThrowStatement(lv.get(0).evalValue(c), Throwables.USER_DEFINED );
                case 2:
                    throw new ThrowStatement(lv.get(1).evalValue(c), Throwables.getTypeForException(lv.get(0).evalValue(c).getString()));
                default:
                    throw new InternalExpressionException("throw() can't accept more than 2 parameters");
            }
        });

        expression.addLazyFunction("try", -1, (c, t, lv) ->
        {
            if (lv.size()==0)
                throw new InternalExpressionException("'try' needs at least an expression block, and either a catch_epr, or a number of pairs of filters and catch_expr");
            try
            {
                Value retval = lv.get(0).evalValue(c, t);
                return (c_, t_) -> retval;
            }
            catch (ProcessedThrowStatement ret)
            {
                if (lv.size() == 1)
                {
                    if (ret.thrownExceptionType != Throwables.USER_DEFINED)
                        throw ret;
                    return (c_, t_) -> Value.NULL;
                }
                if (lv.size() > 3 && lv.size() % 2 == 0)
                {
                    throw new InternalExpressionException("Try-catch block needs the code to run, and either a catch expression for user thrown exceptions, or a number of pairs of filters and catch expressions");
                }
                
                Value val = null; // This is always assigned at some point, just the compiler doesn't know
                
                LazyValue __ = c.getVariable("_");
                c.setVariable("_", (__c, __t) -> ret.data.reboundedTo("_"));

                if (lv.size() == 2)
                {
                    if (ret.thrownExceptionType == Throwables.USER_DEFINED)
                        val = lv.get(1).evalValue(c, t);
                }
                else
                {
                    int pointer = 1;
                    while (pointer < lv.size() -1)
                    {
                        if (ret.thrownExceptionType.isRelevantFor(lv.get(pointer).evalValue(c).getString()))
                        {
                            val = lv.get(pointer + 1).evalValue(c, t);
                            break;
                        }
                        pointer += 2;
                    }
                }
                c.setVariable("_", __);
                
                if (val == null)  // not handled
                {
                    throw ret;
                }
                Value retval = val;
                return (c_, t_) -> retval;
            }
        });
    }
}
