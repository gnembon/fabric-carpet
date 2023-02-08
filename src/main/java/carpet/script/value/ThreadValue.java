package carpet.script.value;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.Tokenizer;
import carpet.script.exception.ExitStatement;
import carpet.script.exception.ExpressionException;
import carpet.script.exception.InternalExpressionException;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import net.minecraft.nbt.Tag;

public class ThreadValue extends Value
{
    private final CompletableFuture<Value> taskFuture;
    private final long id;
    private static long sequence = 0L;

    public ThreadValue(CompletableFuture<Value> taskFuture)
    {
        this.taskFuture = taskFuture;
        this.id = sequence++;
    }

    public ThreadValue(Value pool, FunctionValue function, Expression expr, Tokenizer.Token token, Context ctx, List<Value> args)
    {
        this(getCompletableFutureFromFunction(pool, function, expr, token, ctx, args));
        Thread.yield();
    }

    public static CompletableFuture<Value> getCompletableFutureFromFunction(Value pool, FunctionValue function, Expression expr, Tokenizer.Token token, Context ctx, List<Value> args)
    {
        ExecutorService executor = ctx.host.getExecutor(pool);
        if (executor == null)
        {
            // app is shutting down - no more threads can be spawned.
            return CompletableFuture.completedFuture(Value.NULL);
        }
        else
        {
            return CompletableFuture.supplyAsync(() -> {
                try
                {
                    return function.execute(ctx, Context.NONE, expr, token, args).evalValue(ctx);
                }
                catch (ExitStatement exit)
                {
                    // app stopped
                    return exit.retval;
                }
                catch (ExpressionException exc)
                {
                    ctx.host.handleExpressionException("Thread failed\n", exc);
                    return Value.NULL;
                }
            }, ctx.host.getExecutor(pool));
        }
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
        catch (ExitStatement exit)
        {
            taskFuture.complete(exit.retval);
            return exit.retval;
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
        return o instanceof final ThreadValue tv && tv.id == this.id;
    }

    @Override
    public int compareTo(Value o)
    {
        if (!(o instanceof final ThreadValue tv))
        {
            throw new InternalExpressionException("Cannot compare tasks to other types");
        }
        return (int) (this.id - tv.id);
    }

    @Override
    public int hashCode()
    {
        return Long.hashCode(id);
    }

    @Override
    public Tag toTag(boolean force)
    {
        if (!force)
        {
            throw new NBTSerializableValue.IncompatibleTypeException(this);
        }
        return getValue().toTag(true);
    }

    @Override
    public String getTypeString()
    {
        return "task";
    }
}
