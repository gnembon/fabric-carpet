package carpet.script.value;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.LazyValue;
import carpet.script.Tokenizer;
import carpet.script.exception.ExpressionException;
import carpet.script.exception.InternalExpressionException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadValue extends Value
{
    private CompletableFuture<Value> taskFuture;
    private long id;
    private static long sequence = 0L;
    private static final Map<Value,ThreadPoolExecutor> executorServices = new HashMap<>();
    private static final Map<Value, Object> locks = new ConcurrentHashMap<>();

    public ThreadValue(Value pool, FunctionValue function, Expression expr, Tokenizer.Token token, Context ctx, List<LazyValue> args)
    {
        id = sequence++;
        taskFuture = CompletableFuture.supplyAsync(
            () ->
            {
                try
                {
                    return function.lazyEval(ctx, Context.NONE, expr, token, args).evalValue(ctx);
                }
                catch (ExpressionException exc)
                {
                    ctx.host.handleExpressionException("Thread failed", exc);
                    return Value.NULL;
                }
            },
            executorServices.computeIfAbsent(pool, (v) -> (ThreadPoolExecutor)Executors.newCachedThreadPool())
        );
        Thread.yield();
    }

    @Override
    public String getString()
    {
        return taskFuture.getNow(Value.NULL).getString();
    }

    public Value getValue()
    {
        return taskFuture.getNow(Value.NULL);
    }

    @Override
    public boolean getBoolean()
    {
        return taskFuture.getNow(Value.NULL).getBoolean();
    }

    public Value join()
    {
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

    public static Object getLock(Value name)
    {
        return locks.computeIfAbsent(name, (n) -> new Object());
    }


    public static void shutdown()
    {
        for (ExecutorService exec : executorServices.values())
        {
            exec.shutdown();
            try
            {
                exec.awaitTermination(5000, TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException ignored)
            {
            }
            finally
            {
                exec.shutdownNow();
            }
        }
        executorServices.clear();
        locks.clear();
    }

}
