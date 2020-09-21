package carpet.script.language;

import carpet.script.Expression;
import carpet.script.argument.FunctionArgument;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.NumericValue;
import carpet.script.value.ThreadValue;
import carpet.script.value.Value;

public class Threading
{
    public static void apply(Expression expression)
    {
        expression.addLazyFunctionWithDelegation("task", -1, (c, t, expr, tok, lv) ->
        {
            if (lv.size() == 0)
                throw new InternalExpressionException("'task' requires at least function to call as a parameter");
            FunctionArgument functionArgument = FunctionArgument.findIn(c, expression.module, lv, 0, true, true);
            ThreadValue thread = new ThreadValue(Value.NULL, functionArgument.function, expr, tok, c, functionArgument.args);
            Thread.yield();
            return (cc, tt) -> thread;
        });

        expression.addLazyFunctionWithDelegation("task_thread", -1, (c, t, expr, tok, lv) ->
        {
            if (lv.size() < 2)
                throw new InternalExpressionException("'task' requires at least function to call as a parameter");
            Value queue = lv.get(0).evalValue(c);
            FunctionArgument functionArgument = FunctionArgument.findIn(c, expression.module, lv, 1, true, true);
            ThreadValue thread = new ThreadValue(queue, functionArgument.function, expr, tok, c, functionArgument.args);
            Thread.yield();
            return (cc, tt) -> thread;
        });


        expression.addFunction("task_count", (lv) ->
        {
            if (lv.size() > 0)
            {
                return new NumericValue(ThreadValue.taskCount(lv.get(0)));
            }
            return new NumericValue(ThreadValue.taskCount());
        });

        expression.addUnaryFunction("task_value", (v) ->
        {
            if (!(v instanceof ThreadValue))
                throw new InternalExpressionException("'task_value' could only be used with a task value");
            return ((ThreadValue) v).getValue();
        });

        expression.addUnaryFunction("task_join", (v) ->
        {
            if (!(v instanceof ThreadValue))
                throw new InternalExpressionException("'task_join' could only be used with a task value");
            return ((ThreadValue) v).join();
        });

        expression.addLazyFunction("task_dock", 1, (c, t, lv) ->
        {
            // pass through placeholder
            // implmenetation should dock the task on the main thread.
            return lv.get(0);
        });

        expression.addUnaryFunction("task_completed", (v) ->
        {
            if (!(v instanceof ThreadValue))
                throw new InternalExpressionException("'task_completed' could only be used with a task value");
            return new NumericValue(((ThreadValue) v).isFinished());
        });

        expression.addLazyFunction("synchronize", -1, (c, t, lv) ->
        {
            if (lv.size() == 0) throw new InternalExpressionException("'synchronize' require at least an expression to synchronize");
            Value lockValue = Value.NULL;
            int ind = 0;
            if (lv.size() == 2)
            {
                lockValue = lv.get(0).evalValue(c);
                ind = 1;
            }
            synchronized (ThreadValue.getLock(lockValue))
            {
                Value ret = lv.get(ind).evalValue(c, t);
                return (_c, _t) -> ret;
            }
        });
    }
}
