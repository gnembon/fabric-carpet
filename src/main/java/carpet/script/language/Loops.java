package carpet.script.language;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.LazyValue;
import carpet.script.exception.BreakStatement;
import carpet.script.exception.ContinueStatement;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.AbstractListValue;
import carpet.script.value.ListValue;
import carpet.script.value.NumericValue;
import carpet.script.value.Value;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Loops
{
    public static void apply(Expression expression)
    {
        // condition and expression will get a bound '_i'
        // returns last successful expression or false
        // while(cond, limit, expr) => ??
        expression.addImpureFunction("break", lv ->
        {
            if (lv.isEmpty())
            {
                throw new BreakStatement(null);
            }
            if (lv.size() == 1)
            {
                throw new BreakStatement(lv.get(0));
            }
            throw new InternalExpressionException("'break' can only be called with zero or one argument");
        });

        expression.addImpureFunction("continue", lv ->
        {
            if (lv.isEmpty())
            {
                throw new ContinueStatement(null);
            }
            if (lv.size() == 1)
            {
                throw new ContinueStatement(lv.get(0));
            }
            throw new InternalExpressionException("'continue' can only be called with zero or one argument");
        });

        // lazy
        expression.addLazyFunction("while", -1, (c, t, lv) ->
        {
            if (lv.size() == 2) { // lets do nasty way so performance is not affected (might be super unnecessary, but hey)
                LazyValue condition = lv.get(0);
                LazyValue expr = lv.get(1);
                long i = 0;
                Value lastOne = Value.NULL;
                //scoping
                LazyValue defaultVal = c.getVariable("_");
                c.setVariable("_", (cc, tt) -> new NumericValue(0).bindTo("_"));
                while (condition.evalValue(c, Context.BOOLEAN).getBoolean())
                {
                    try
                    {
                        lastOne = expr.evalValue(c, t);
                    }
                    catch (BreakStatement | ContinueStatement stmt)
                    {
                        if (stmt.retval != null)
                        {
                            lastOne = stmt.retval;
                        }
                        if (stmt instanceof BreakStatement)
                        {
                            break;
                        }
                    }
                    i++;
                    long seriously = i;
                    c.setVariable("_", (cc, tt) -> new NumericValue(seriously).bindTo("_"));
                }
                //revering scope
                c.setVariable("_", defaultVal);
                Value lastValueNoKidding = lastOne;
                return (cc, tt) -> lastValueNoKidding;
            }
            long limit = NumericValue.asNumber(lv.get(1).evalValue(c)).getLong();
            LazyValue condition = lv.get(0);
            LazyValue expr = lv.get(2);
            long i = 0;
            Value lastOne = Value.NULL;
            //scoping
            LazyValue defaultVal = c.getVariable("_");
            c.setVariable("_", (cc, tt) -> new NumericValue(0).bindTo("_"));
            while (i < limit && condition.evalValue(c, Context.BOOLEAN).getBoolean())
            {
                try
                {
                    lastOne = expr.evalValue(c, t);
                }
                catch (BreakStatement | ContinueStatement stmt)
                {
                    if (stmt.retval != null)
                    {
                        lastOne = stmt.retval;
                    }
                    if (stmt instanceof BreakStatement)
                    {
                        break;
                    }
                }
                i++;
                long seriously = i;
                c.setVariable("_", (cc, tt) -> new NumericValue(seriously).bindTo("_"));
            }
            //revering scope
            c.setVariable("_", defaultVal);
            Value lastValueNoKidding = lastOne;
            return (cc, tt) -> lastValueNoKidding;
        });

        // loop(Num, expr) => lastdefaultValue
        // expr receives bounded variable '_' indicating iteration
        expression.addLazyFunction("loop", 2, (c, t, lv) ->
        {
            long limit = NumericValue.asNumber(lv.get(0).evalValue(c, Context.NONE)).getLong();
            Value lastOne = Value.NULL;
            LazyValue expr = lv.get(1);
            //scoping
            LazyValue defaultVal = c.getVariable("_");
            for (long i = 0; i < limit; i++)
            {
                long whyYouAsk = i;
                c.setVariable("_", (cc, tt) -> new NumericValue(whyYouAsk).bindTo("_"));
                try
                {
                    lastOne = expr.evalValue(c, t);
                }
                catch (BreakStatement | ContinueStatement stmt)
                {
                    if (stmt.retval != null)
                    {
                        lastOne = stmt.retval;
                    }
                    if (stmt instanceof BreakStatement)
                    {
                        break;
                    }
                }
            }
            //revering scope
            c.setVariable("_", defaultVal);
            Value trulyLastOne = lastOne;
            return (cc, tt) -> trulyLastOne;
        });

        // map(list or Num, expr) => list_results
        // receives bounded variable '_' with the expression
        expression.addLazyFunction("map", 2, (c, t, lv) ->
        {
            Value rval = lv.get(0).evalValue(c, Context.NONE);
            if (rval.isNull())
            {
                return ListValue.lazyEmpty();
            }
            if (!(rval instanceof final AbstractListValue alv))
            {
                throw new InternalExpressionException("First argument of 'map' function should be a list or iterator");
            }
            Iterator<Value> iterator = alv.iterator();
            LazyValue expr = lv.get(1);
            //scoping
            LazyValue defaultVal = c.getVariable("_");
            LazyValue iterVal = c.getVariable("_i");
            List<Value> result = new ArrayList<>();
            for (int i = 0; iterator.hasNext(); i++)
            {
                Value next = iterator.next();
                if(next == Value.EOL) {
                    continue;
                }
                String variable = next.boundVariable;
                next.bindTo("_");
                int doYouReally = i;
                c.setVariable("_", (cc, tt) -> next);
                c.setVariable("_i", (cc, tt) -> new NumericValue(doYouReally).bindTo("_i"));
                try
                {
                    result.add(expr.evalValue(c, t));
                }
                catch (BreakStatement | ContinueStatement stmt)
                {
                    if (stmt.retval != null)
                    {
                        result.add(stmt.retval);
                    }
                    if (stmt instanceof BreakStatement)
                    {
                        next.boundVariable = variable;
                        break;
                    }
                }
                next.boundVariable = variable;
            }
            ((AbstractListValue) rval).fatality();
            Value ret = ListValue.wrap(result);
            //revering scope
            c.setVariable("_", defaultVal);
            c.setVariable("_i", iterVal);
            return (cc, tt) -> ret;
        });

        // grep(list or num, expr) => list
        // receives bounded variable '_' with the expression, and "_i" with index
        // produces list of values for which the expression is true
        expression.addLazyFunction("filter", 2, (c, t, lv) ->
        {
            Value rval = lv.get(0).evalValue(c, Context.NONE);
            if (rval.isNull())
            {
                return ListValue.lazyEmpty();
            }
            if (!(rval instanceof final AbstractListValue alv))
            {
                throw new InternalExpressionException("First argument of 'filter' function should be a list or iterator");
            }
            Iterator<Value> iterator = alv.iterator();
            LazyValue expr = lv.get(1);
            //scoping
            LazyValue defaultVal = c.getVariable("_");
            LazyValue iterVal = c.getVariable("_i");
            List<Value> result = new ArrayList<>();
            for (int i = 0; iterator.hasNext(); i++)
            {
                Value next = iterator.next();
                if(next == Value.EOL) {
                    continue;
                }
                String veriable = next.boundVariable;
                next.bindTo("_");
                int seriously = i;
                c.setVariable("_", (cc, tt) -> next);
                c.setVariable("_i", (cc, tt) -> new NumericValue(seriously).bindTo("_i"));
                try
                {
                    if (expr.evalValue(c, Context.BOOLEAN).getBoolean())
                    {
                        result.add(next);
                    }
                }
                catch (BreakStatement | ContinueStatement stmt)
                {
                    if (stmt.retval != null && stmt.retval.getBoolean())
                    {
                        result.add(next);
                    }
                    if (stmt instanceof BreakStatement)
                    {
                        next.boundVariable = veriable;
                        break;
                    }
                }
                next.boundVariable = veriable;
            }
            ((AbstractListValue) rval).fatality();
            Value ret = ListValue.wrap(result);
            //revering scope
            c.setVariable("_", defaultVal);
            c.setVariable("_i", iterVal);
            return (cc, tt) -> ret;
        });

        // first(list, expr) => elem or null
        // receives bounded variable '_' with the expression, and "_i" with index
        // returns first element on the list for which the expr is true
        expression.addLazyFunction("first", 2, (c, t, lv) ->
        {
            Value rval = lv.get(0).evalValue(c, Context.NONE);
            if (rval.isNull())
            {
                return LazyValue.NULL;
            }
            if (!(rval instanceof final AbstractListValue alv))
            {
                throw new InternalExpressionException("First argument of 'first' function should be a list or iterator");
            }
            Iterator<Value> iterator = alv.iterator();
            LazyValue expr = lv.get(1);
            //scoping
            LazyValue defaultVal = c.getVariable("_");
            LazyValue iterVal = c.getVariable("_i");
            Value result = Value.NULL;
            for (int i = 0; iterator.hasNext(); i++)
            {
                Value next = iterator.next();
                if(next == Value.EOL) {
                    continue;
                }
                String variable = next.boundVariable;
                next.bindTo("_");
                int seriously = i;
                c.setVariable("_", (cc, tt) -> next);
                c.setVariable("_i", (cc, tt) -> new NumericValue(seriously).bindTo("_i"));
                try
                {
                    if (expr.evalValue(c, Context.BOOLEAN).getBoolean())
                    {
                        result = next;
                        next.boundVariable = variable;
                        break;
                    }
                }
                catch (BreakStatement stmt)
                {
                    result = stmt.retval == null ? next : stmt.retval;
                    next.boundVariable = variable;
                    break;
                }
                catch (ContinueStatement ignored)
                {
                    throw new InternalExpressionException("'continue' inside 'first' function has no sense");
                }
                next.boundVariable = variable;
            }
            //revering scope
            ((AbstractListValue) rval).fatality();
            Value whyWontYouTrustMeJava = result;
            c.setVariable("_", defaultVal);
            c.setVariable("_i", iterVal);
            return (cc, tt) -> whyWontYouTrustMeJava;
        });

        // all(list, expr) => boolean
        // receives bounded variable '_' with the expression, and "_i" with index
        // returns true if expr is true for all items
        expression.addLazyFunction("all", 2, (c, t, lv) ->
        {
            Value rval = lv.get(0).evalValue(c, Context.NONE);
            if (rval.isNull())
            {
                return LazyValue.TRUE;
            }
            if (!(rval instanceof final AbstractListValue alv))
            {
                throw new InternalExpressionException("First argument of 'all' function should be a list or iterator");
            }
            Iterator<Value> iterator = alv.iterator();
            LazyValue expr = lv.get(1);
            //scoping
            LazyValue defaultVal = c.getVariable("_");
            LazyValue iterVal = c.getVariable("_i");
            LazyValue result = LazyValue.TRUE;
            for (int i = 0; iterator.hasNext(); i++)
            {
                Value next = iterator.next();
                if(next == Value.EOL) {
                    continue;
                }
                String variable = next.boundVariable;
                next.bindTo("_");
                int seriously = i;
                c.setVariable("_", (cc, tt) -> next);
                c.setVariable("_i", (cc, tt) -> new NumericValue(seriously).bindTo("_i"));
                if (!expr.evalValue(c, Context.BOOLEAN).getBoolean())
                {
                    result = LazyValue.FALSE;
                    next.boundVariable = variable;
                    break;
                }
                next.boundVariable = variable;
            }
            //revering scope
            ((AbstractListValue) rval).fatality();
            c.setVariable("_", defaultVal);
            c.setVariable("_i", iterVal);
            return result;
        });

        // runs traditional for(init, condition, increment, body) tri-argument for loop with body in between
        expression.addLazyFunction("c_for", 4, (c, t, lv) ->
        {
            LazyValue initial = lv.get(0);
            LazyValue condition = lv.get(1);
            LazyValue increment = lv.get(2);
            LazyValue body = lv.get(3);
            int iterations = 0;
            for (initial.evalValue(c, Context.VOID); condition.evalValue(c, Context.BOOLEAN).getBoolean(); increment.evalValue(c, Context.VOID))
            {
                try
                {
                    body.evalValue(c, Context.VOID);
                }
                catch (BreakStatement stmt)
                {
                    break;
                }
                catch (ContinueStatement ignored)
                {
                }
                iterations++;
            }
            int finalIterations = iterations;
            return (cc, tt) -> new NumericValue(finalIterations);
        });

        // similar to map, but returns total number of successes
        // for(list, expr) => success_count
        // can be substituted for first and all, but first is more efficient and all doesn't require knowing list size
        expression.addLazyFunction("for", 2, (c, t, lv) ->
        {
            Value rval = lv.get(0).evalValue(c, Context.NONE);
            if (rval.isNull())
            {
                return LazyValue.ZERO;
            }
            if (!(rval instanceof final AbstractListValue alv))
            {
                throw new InternalExpressionException("First argument of 'for' function should be a list or iterator");
            }
            Iterator<Value> iterator = alv.iterator();
            LazyValue expr = lv.get(1);
            //scoping
            LazyValue defaultVal = c.getVariable("_");
            LazyValue iterVal = c.getVariable("_i");
            int successCount = 0;
            for (int i = 0; iterator.hasNext(); i++)
            {
                Value next = iterator.next();
                if(next == Value.EOL) {
                    continue;
                }
                String variable = next.boundVariable;
                next.bindTo("_");
                int seriously = i;
                c.setVariable("_", (cc, tt) -> next);
                c.setVariable("_i", (cc, tt) -> new NumericValue(seriously).bindTo("_i"));
                Value result = Value.FALSE;
                try
                {
                    result = expr.evalValue(c, t);
                }
                catch (BreakStatement | ContinueStatement stmt)
                {
                    if (stmt.retval != null)
                    {
                        result = stmt.retval;
                    }
                    if (stmt instanceof BreakStatement)
                    {
                        next.boundVariable = variable;
                        break;
                    }
                }
                if (t != Context.VOID && result.getBoolean())
                {
                    successCount++;
                }
                next.boundVariable = variable;
            }
            //revering scope
            ((AbstractListValue) rval).fatality();
            c.setVariable("_", defaultVal);
            c.setVariable("_i", iterVal);
            long promiseWontChange = successCount;
            return (cc, tt) -> new NumericValue(promiseWontChange);
        });


        // reduce(list, expr, ?acc) => value
        // reduces values in the list with expression that gets accumulator
        // each iteration expr receives acc - accumulator, and '_' - current list value
        // returned value is substituted to the accumulator
        expression.addLazyFunction("reduce", 3, (c, t, lv) ->
        {

            Value rval = lv.get(0).evalValue(c, Context.NONE);
            if (rval.isNull())
            {
                return ListValue.lazyEmpty();
            }
            if (!(rval instanceof final AbstractListValue alv))
            {
                throw new InternalExpressionException("First argument of 'reduce' should be a list or iterator");
            }
            LazyValue expr = lv.get(1);
            Value acc = lv.get(2).evalValue(c, Context.NONE);
            Iterator<Value> iterator = alv.iterator();

            if (!iterator.hasNext())
            {
                Value seriouslyWontChange = acc;
                return (cc, tt) -> seriouslyWontChange;
            }

            //scoping
            LazyValue defaultVal = c.getVariable("_");
            LazyValue accumulatorVal = c.getVariable("_a");
            LazyValue iterVal = c.getVariable("_i");

            for (int i = 0; iterator.hasNext(); i++)
            {
                Value next = iterator.next();
                if(next == Value.EOL) {
                    continue;
                }
                String variable = next.boundVariable;
                next.bindTo("_");
                Value promiseWontChangeYou = acc;
                int seriously = i;
                c.setVariable("_a", (cc, tt) -> promiseWontChangeYou.bindTo("_a"));
                c.setVariable("_", (cc, tt) -> next);
                c.setVariable("_i", (cc, tt) -> new NumericValue(seriously).bindTo("_i"));
                try
                {
                    acc = expr.evalValue(c, t);
                }
                catch (BreakStatement | ContinueStatement stmt)
                {
                    if (stmt.retval != null)
                    {
                        acc = stmt.retval;
                    }
                    if (stmt instanceof BreakStatement)
                    {
                        next.boundVariable = variable;
                        break;
                    }
                }
                next.boundVariable = variable;
            }
            //reverting scope
            ((AbstractListValue) rval).fatality();
            c.setVariable("_a", accumulatorVal);
            c.setVariable("_", defaultVal);
            c.setVariable("_i", iterVal);

            Value hopeItsEnoughPromise = acc;
            return (cc, tt) -> hopeItsEnoughPromise;
        });
    }
}
