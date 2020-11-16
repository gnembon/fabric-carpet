package carpet.script.value;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.Tokenizer;
import carpet.script.exception.ExitStatement;
import carpet.script.exception.ExpressionException;
import carpet.script.exception.InternalExpressionException;
import net.minecraft.nbt.Tag;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

public class ThreadValue extends Value
{
    private final CompletableFuture<Value> taskFuture;
    private final long id;
    private static long sequence = 0L;

    public ThreadValue(Value pool, FunctionValue function, Expression expr, Tokenizer.Token token, Context ctx, List<Value> args)
    {
        id = sequence++;
        ExecutorService executor = ctx.host.getExecutor(pool);
        if (executor == null)
        {
            // app is shutting down - no more threads can be spawned.
            taskFuture = CompletableFuture.completedFuture(Value.NULL);
        }
        else
        {
            taskFuture = CompletableFuture.supplyAsync(
                    () ->
                    {
                        try
                        {
                            return function.lazyEval(ctx, Context.NONE, expr, token, FunctionValue.lazify(args)).evalValue(ctx);
                        }
                        catch (ExitStatement exit)
                        {
                            // app stopped
                            return exit.retval;
                        }
                        catch (ExpressionException exc)
                        {
                            ctx.host.handleExpressionException("Thread failed", exc);
                            return Value.NULL;
                        }

                    },
                    ctx.host.getExecutor(pool)
            );
        }
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

    @Override
    public Tag toTag(boolean force)
    {
        if (!force) throw new NBTSerializableValue.IncompatibleTypeException(this);
        return getValue().toTag(true);
    }

    @Override
    public String getTypeString()
    {
        return "task";
    }
}
