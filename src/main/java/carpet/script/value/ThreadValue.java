package carpet.script.value;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.Tokenizer;
import carpet.script.exception.InternalExpressionException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;

public class ThreadValue extends Value
{
    private CompletableFuture<Value> taskFuture;
    private long id;
    private final Object lock;
    private final boolean[] started;
    private Supplier<Value>task;
    private static long sequence = 0L;

    private static final Map<Value,ThreadPoolExecutor> executorServices = new HashMap<>();

    public ThreadValue(Value pool, FunctionValue function, Expression expr, Tokenizer.Token token, Context ctx)
    {
        id = sequence++;
        lock = new Object();
        started = new boolean[]{false};
        task = () ->
        {
            if (!started[0])
            {
                started[0] = true;
                return function.lazyEval(ctx, Context.NONE, expr, token, Collections.emptyList()).evalValue(ctx);
            }
            return null;
        };
        taskFuture = CompletableFuture.supplyAsync(
                () -> { synchronized (lock) { return task.get(); } },
                executorServices.computeIfAbsent(pool, (v) -> (ThreadPoolExecutor)Executors.newCachedThreadPool())
        );
        Thread.yield();
    }

    @Override
    public String getString()
    {
        return taskFuture.getNow(Value.NULL).getString();
    }

    @Override
    public boolean getBoolean()
    {
        return taskFuture.getNow(Value.NULL).getBoolean();
    }

    public Value join()
    {
        if (!started[0])
        {
            synchronized (lock)
            {
                if (!started[0])
                {
                    Value res = task.get();
                    taskFuture.complete(res);
                    return res;
                }
            }
        }
        try
        {
            return taskFuture.get();
        }
        catch (InterruptedException | ExecutionException e)
        {
            return Value.NULL;
        }
    }

    public boolean isFinished()
    {
        return taskFuture.isDone();
    }

    public void stop()
    {
        synchronized (lock)
        {
            if (!taskFuture.isDone())
            {
                started[0] = true;
                taskFuture.complete(Value.NULL);
            }
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof ThreadValue))
            return false;
        return ((ThreadValue) o).id == this.id;
    }

    @Override
    public int compareTo(Value o)
    {
        if (!(o instanceof ThreadValue))
            throw new InternalExpressionException("Cannot compare tasks to other types");
        return (int) (this.id - ((ThreadValue) o).id);
    }

    @Override
    public int hashCode()
    {
        return Long.hashCode(id);
    }

    public static int taskCount()
    {
        return executorServices.values().stream().map(ThreadPoolExecutor::getActiveCount).reduce(0, Integer::sum);
    }

    public static int taskCount(Value pool)
    {
        if (executorServices.containsKey(pool))
        {
            return executorServices.get(pool).getActiveCount();
        }
        return 0;
    }
}
