package carpet.script.value;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.Tokenizer;
import carpet.script.exception.ExitStatement;
import carpet.script.exception.ExpressionException;
import carpet.script.exception.InternalExpressionException;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.Tag;

public class ThreadValue extends LazyListValue
{
    private final CompletableFuture<Value> taskFuture;
    private final long id;
    private static long sequence = 0L;
    private final Deque<Value> coState = new ArrayDeque<>();
    private final AtomicReference<Value> coLock = new AtomicReference<>(Value.EOL);
    public final boolean isCoroutine;

    public ThreadValue(Value pool, FunctionValue function, Expression expr, Tokenizer.Token token, Context ctx, List<Value> args)
    {
        this.id = sequence++;
        this.isCoroutine = ctx.host.canSynchronouslyExecute();
        this.taskFuture = getCompletableFutureFromFunction(pool, function, expr, token, ctx, args);

        Thread.yield();
    }

    public CompletableFuture<Value> getCompletableFutureFromFunction(Value pool, FunctionValue function, Expression expr, Tokenizer.Token token, Context ctx, List<Value> args)
    {
        ExecutorService executor = ctx.host.getExecutor(pool);
        ThreadValue callingThread = isCoroutine ? this : null;
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
                    return function.execute(ctx, Context.NONE, expr, token, args, callingThread).evalValue(ctx);
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
        return o instanceof ThreadValue tv && tv.id == this.id;
    }

    @Override
    public int compareTo(Value o)
    {
        if (!(o instanceof ThreadValue tv))
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
    public Tag toTag(boolean force, RegistryAccess regs)
    {
        if (!force)
        {
            throw new NBTSerializableValue.IncompatibleTypeException(this);
        }
        return getValue().toTag(true, regs);
    }

    @Override
    public String getTypeString()
    {
        return "task";
    }

    @Override
    public void fatality()
    {
        // we signal that won't be interested in the co-thread anymore
        // but threads run client code, so we can't just kill them
    }

    @Override
    public void reset()
    {
        //throw new InternalExpressionException("Illegal operation on a task");
    }


    @Override
    public Iterator<Value> iterator()
    {
        if (!isCoroutine)
        {
            throw new InternalExpressionException("Cannot iterate over this task");
        }
        return this;
    }

    @Override
    public boolean hasNext()
    {
        return !(coState.isEmpty() && taskFuture.isDone());
    }

    @Override
    public Value next()
    {
        Value popped = null;
        synchronized (coState)
        {
            while (true)
            {
                if (!coState.isEmpty())
                {
                    popped = coState.pop();
                }
                else if (taskFuture.isDone())
                {
                    popped = Value.EOL;
                }
                if (popped != null)
                {
                    break;
                }
                try
                {
                    coState.wait(1);
                }
                catch (InterruptedException ignored)
                {
                }
            }
            coState.notifyAll();
        }
        return popped;
    }

    public void send(Value value)
    {
        synchronized (coLock)
        {
            coLock.set(value);
            coLock.notifyAll();
        }
    }

    public Value ping(Value value, boolean lock)
    {
        synchronized (coState)
        {
            try
            {
                if (!lock)
                {
                    coState.add(value);
                    return Value.NULL;
                }
                while (true)
                {
                    if (coState.isEmpty())
                    {
                        coState.add(value);
                        break;
                    }
                    try
                    {
                        coState.wait(1);
                    }

                    catch (InterruptedException ignored)
                    {
                    }
                }
            }
            finally
            {
                coState.notifyAll();
            }
        }

        // locked mode

        synchronized (coLock)
        {
            Value ret;
            try
            {
                while (true)
                {
                    Value current = coLock.get();
                    if (current != Value.EOL)
                    {
                        ret = current;
                        coLock.set(Value.EOL);
                        break;
                    }
                    try
                    {
                        coLock.wait(1);
                    }
                    catch (InterruptedException ignored)
                    {
                    }
                }
            }
            finally
            {
                coLock.notifyAll();
            }
            return ret;
        }
    }
}
