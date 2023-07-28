package carpet.script.language;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.argument.FunctionArgument;
import carpet.script.exception.ExitStatement;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.BooleanValue;
import carpet.script.value.NumericValue;
import carpet.script.value.ThreadValue;
import carpet.script.value.Value;

public class Threading
{
    public static void apply(Expression expression)
    {
        expression.addFunctionWithDelegation("task", -1, false, false, (c, t, expr, tok, lv) ->
        {
            if (lv.isEmpty())
            {
                throw new InternalExpressionException("'task' requires at least function to call as a parameter");
            }
            FunctionArgument functionArgument = FunctionArgument.findIn(c, expression.module, lv, 0, false, true);
            ThreadValue thread = new ThreadValue(Value.NULL, functionArgument.function, expr, tok, c, functionArgument.checkedArgs());
            Thread.yield();
            return thread;
        });

        expression.addFunctionWithDelegation("task_thread", -1, false, false, (c, t, expr, tok, lv) ->
        {
            if (lv.size() < 2)
            {
                throw new InternalExpressionException("'task' requires at least function to call as a parameter");
            }
            Value queue = lv.get(0);
            FunctionArgument functionArgument = FunctionArgument.findIn(c, expression.module, lv, 1, false, true);
            ThreadValue thread = new ThreadValue(queue, functionArgument.function, expr, tok, c, functionArgument.checkedArgs());
            Thread.yield();
            return thread;
        });


        expression.addContextFunction("task_count", -1, (c, t, lv) ->
                (!lv.isEmpty()) ? new NumericValue(c.host.taskCount(lv.get(0))) : new NumericValue(c.host.taskCount()));

        expression.addUnaryFunction("task_value", v ->
        {
            if (!(v instanceof final ThreadValue tv))
            {
                throw new InternalExpressionException("'task_value' could only be used with a task value");
            }
            return tv.getValue();
        });

        expression.addUnaryFunction("task_join", v ->
        {
            if (!(v instanceof final ThreadValue tv))
            {
                throw new InternalExpressionException("'task_join' could only be used with a task value");
            }
            return tv.join();
        });

        expression.addLazyFunction("task_dock", 1, (c, t, lv) ->
            // pass through placeholder
            // implmenetation should dock the task on the main thread.
            lv.get(0)
        );

        expression.addUnaryFunction("task_completed", v ->
        {
            if (!(v instanceof final ThreadValue tv))
            {
                throw new InternalExpressionException("'task_completed' could only be used with a task value");
            }
            return BooleanValue.of(tv.isFinished());
        });

        // lazy cause expr is evaluated in the same type
        expression.addLazyFunction("synchronize", (c, t, lv) ->
        {
            if (lv.isEmpty())
            {
                throw new InternalExpressionException("'synchronize' require at least an expression to synchronize");
            }
            Value lockValue = Value.NULL;
            int ind = 0;
            if (lv.size() == 2)
            {
                lockValue = lv.get(0).evalValue(c);
                ind = 1;
            }
            synchronized (c.host.getLock(lockValue))
            {
                Value ret = lv.get(ind).evalValue(c, t);
                return (ct, tt) -> ret;
            }
        });

        // lazy since exception expression is very conditional
        expression.addLazyFunction("sleep", (c, t, lv) ->
        {
            long time = lv.isEmpty() ? 0L : NumericValue.asNumber(lv.get(0).evalValue(c)).getLong();
            boolean interrupted = false;
            try
            {
                if (Thread.interrupted())
                {
                    interrupted = true;
                }
                if (time > 0)
                {
                    Thread.sleep(time);
                }
                Thread.yield();
            }
            catch (InterruptedException ignored)
            {
                interrupted = true;
            }
            if (interrupted)
            {
                Value exceptionally = Value.NULL;
                if (lv.size() > 1)
                {
                    exceptionally = lv.get(1).evalValue(c);
                }
                throw new ExitStatement(exceptionally);
            }
            return (cc, tt) -> new NumericValue(time); // pass through for variables
        });

        expression.addLazyFunction("yield", (c, t, lv) ->
        {
            if (c.getThreadContext() == null)
            {
                throw new InternalExpressionException("'yield' can only be used in a task");
            }
            if (lv.isEmpty())
            {
                throw new InternalExpressionException("'yield' requires at least one argument");
            }
            boolean lock = lv.size() > 1 && lv.get(1).evalValue(c, Context.BOOLEAN).getBoolean();
            Value value = lv.get(0).evalValue(c);
            Value ret = c.getThreadContext().ping(value, lock);
            return (cc, tt) -> ret;
        });

        expression.addLazyFunction("task_send", 2, (c, t, lv) ->
        {
            Value threadValue = lv.get(0).evalValue(c);
            if (!(threadValue instanceof ThreadValue thread))
            {
                throw new InternalExpressionException("'task_next' requires a task value");
            }
            if (!thread.isCoroutine)
            {
                throw new InternalExpressionException("'task_next' requires a coroutine task value");
            }
            Value ret = lv.get(1).evalValue(c);
            thread.send(ret);
            return (cc, tt) -> Value.NULL;
        });

        expression.addLazyFunction("task_await", 1, (c, t, lv) ->
        {
            Value threadValue = lv.get(0).evalValue(c);
            if (!(threadValue instanceof ThreadValue thread))
            {
                throw new InternalExpressionException("'task_await' requires a task value");
            }
            if (!thread.isCoroutine)
            {
                throw new InternalExpressionException("'task_await' requires a coroutine task value");
            }
            Value ret = thread.next();
            return ret == Value.EOL ? ((cc, tt) -> Value.NULL) : ((cc, tt) -> ret);
        });

        expression.addLazyFunction("task_ready", 1, (c, t, lv) ->
        {
            Value threadValue = lv.get(0).evalValue(c);
            if (!(threadValue instanceof ThreadValue thread))
            {
                throw new InternalExpressionException("'task_ready' requires a task value");
            }
            boolean ret = thread.isCoroutine && thread.hasNext();
            return (cc, tt) -> BooleanValue.of(ret);
        });
    }
}
