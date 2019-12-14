package carpet.script.value;

import carpet.script.Context;
import carpet.script.LazyValue;
import carpet.script.exception.InternalExpressionException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadValue extends Value
{
    CompletableFuture<Value> taskFuture;
    private long id;
    private static long sequence = 0L;

    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    public ThreadValue(LazyValue expression, Context ctx)
    {
        id = sequence++;
        taskFuture = CompletableFuture.supplyAsync(
                () -> expression.evalValue(ctx),
                executorService
        );
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
        taskFuture.complete(Value.NULL);
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
            throw new InternalExpressionException("Cannot compare ");
        return (int) (this.id - ((ThreadValue) o).id);
    }
}
