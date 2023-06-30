package carpet.script.language;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.LazyValue;
import carpet.script.exception.ExitStatement;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.ProcessedThrowStatement;
import carpet.script.exception.ThrowStatement;
import carpet.script.exception.Throwables;
import carpet.script.value.ListValue;
import carpet.script.value.MapValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;

import java.util.Map;
import java.util.stream.Collectors;

public class ControlFlow
{
    public static void apply(Expression expression) // public just to get the javadoc right
    {
        // needs to be lazy cause of custom contextualization
        expression.addLazyBinaryOperator(";", Operators.precedence.get("nextop;"), true, true, t -> Context.Type.VOID, (c, t, lv1, lv2) ->
        {
            lv1.evalValue(c, Context.VOID);
            Value v2 = lv2.evalValue(c, t);
            return (cc, tt) -> v2;
        });

        expression.addPureLazyFunction("then", -1, t -> Context.Type.VOID, (c, t, lv) -> {
            int imax = lv.size() - 1;
            for (int i = 0; i < imax; i++)
            {
                lv.get(i).evalValue(c, Context.VOID);
            }
            Value v = lv.get(imax).evalValue(c, t);
            return (cc, tt) -> v;
        });
        expression.addFunctionalEquivalence(";", "then");


        // obvious lazy due to conditional evaluation of arguments
        expression.addLazyFunction("if", (c, t, lv) ->
        {
            if (lv.size() < 2)
            {
                throw new InternalExpressionException("'if' statement needs to have at least one condition and one case");
            }
            for (int i = 0; i < lv.size() - 1; i += 2)
            {
                if (lv.get(i).evalValue(c, Context.BOOLEAN).getBoolean())
                {
                    Value ret = lv.get(i + 1).evalValue(c, t);
                    return (cc, tt) -> ret;
                }
            }
            if (lv.size() % 2 == 1)
            {
                Value ret = lv.get(lv.size() - 1).evalValue(c, t);
                return (cc, tt) -> ret;
            }
            return (cc, tt) -> Value.NULL;
        });

        expression.addImpureFunction("exit", lv -> {
            throw new ExitStatement(lv.isEmpty() ? Value.NULL : lv.get(0));
        });

        expression.addImpureFunction("throw", lv ->
        {
            switch (lv.size())
            {
                case 0 -> throw new ThrowStatement(Value.NULL, Throwables.USER_DEFINED);
                case 1 -> throw new ThrowStatement(lv.get(0), Throwables.USER_DEFINED);
                case 2 -> throw new ThrowStatement(lv.get(1), Throwables.getTypeForException(lv.get(0).getString()));
                case 3 -> throw new ThrowStatement(lv.get(2), Throwables.getTypeForException(lv.get(1).getString()), lv.get(0).getString());
                default -> throw new InternalExpressionException("throw() can't accept more than 3 parameters");
            }
        });

        // needs to be lazy since execution of parameters but first one are conditional
        expression.addLazyFunction("try", (c, t, lv) ->
        {
            if (lv.isEmpty())
            {
                throw new InternalExpressionException("'try' needs at least an expression block, and either a catch_epr, or a number of pairs of filters and catch_expr");
            }
            try
            {
                Value retval = lv.get(0).evalValue(c, t);
                return (ct, tt) -> retval;
            }
            catch (ProcessedThrowStatement ret)
            {
                if (lv.size() == 1)
                {
                    if (!ret.thrownExceptionType.isUserException())
                    {
                        throw ret;
                    }
                    return (ct, tt) -> Value.NULL;
                }
                if (lv.size() > 3 && lv.size() % 2 == 0)
                {
                    throw new InternalExpressionException("Try-catch block needs the code to run, and either a catch expression for user thrown exceptions, or a number of pairs of filters and catch expressions");
                }

                Value val = null; // This is always assigned at some point, just the compiler doesn't know

                LazyValue defaultVal = c.getVariable("_");
                c.setVariable("_", (ct, tt) -> ret.data.reboundedTo("_"));
                LazyValue trace = c.getVariable("_trace");
                c.setVariable("_trace", (ct, tt) -> MapValue.wrap(Map.of(
                        StringValue.of("stack"), ListValue.wrap(ret.stack.stream().map(f -> ListValue.of(
                                StringValue.of(f.getModule().name()),
                                StringValue.of(f.getString()),
                                NumericValue.of(f.getToken().lineno + 1),
                                NumericValue.of(f.getToken().linepos + 1)
                        ))),

                        StringValue.of("locals"), MapValue.wrap(ret.context.variables.entrySet().stream().filter(e -> !e.getKey().equals("_trace")).collect(Collectors.toMap(
                                e -> StringValue.of(e.getKey()),
                                e -> e.getValue().evalValue(ret.context)
                        ))),
                        StringValue.of("token"), ListValue.of(
                                StringValue.of(ret.token.surface),
                                NumericValue.of(ret.token.lineno + 1),
                                NumericValue.of(ret.token.linepos + 1)
                        )
                )));

                if (lv.size() == 2)
                {
                    if (ret.thrownExceptionType.isUserException())
                    {
                        val = lv.get(1).evalValue(c, t);
                    }
                }
                else
                {
                    int pointer = 1;
                    while (pointer < lv.size() - 1)
                    {
                        if (ret.thrownExceptionType.isRelevantFor(lv.get(pointer).evalValue(c).getString()))
                        {
                            val = lv.get(pointer + 1).evalValue(c, t);
                            break;
                        }
                        pointer += 2;
                    }
                }
                c.setVariable("_", defaultVal);
                if (trace != null)
                {
                    c.setVariable("_trace", trace);
                }
                else
                {
                    c.delVariable("_trace");
                }
                if (val == null)  // not handled
                {
                    throw ret;
                }
                Value retval = val;
                return (ct, tt) -> retval;
            }
        });
    }
}
