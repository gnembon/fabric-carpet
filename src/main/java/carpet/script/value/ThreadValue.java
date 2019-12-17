package carpet.script.value;

import carpet.CarpetSettings;
import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.Tokenizer;
import carpet.script.exception.InternalExpressionException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;

public class ThreadValue extends Value
{
    private CompletableFuture<Value> taskFuture;
    //public static ThreadLocal<Boolean> joining = ThreadLocal.withInitial(() -> false);
    public final static Map<Thread,Integer> waitingTreads = new ConcurrentHashMap<>();
    public static Map<Thread,Integer> joinRequestedThreads = new ConcurrentHashMap<>();
    private long id;
    private final Object lock;
    //private final boolean[] started;
    private Thread executingThread;
    private Supplier<Value>task;
    private static long sequence = 0L;

    private static final Map<Value,ThreadPoolExecutor> executorServices = new HashMap<>();

    public ThreadValue(Value pool, FunctionValue function, Expression expr, Tokenizer.Token token, Context ctx)
    {
        id = sequence++;
        lock = new Object();
        executingThread = null;
        //started = new boolean[]{false};
        task = () ->
        {
            if (executingThread == null)
            {
                CarpetSettings.LOG.error("Setting the thread");
                executingThread = Thread.currentThread();
                return function.lazyEval(ctx, Context.NONE, expr, token, Collections.emptyList()).evalValue(ctx);
            }
            return null;
        };
        taskFuture = CompletableFuture.supplyAsync(
                () -> { synchronized (lock) {  CarpetSettings.LOG.error("getting in future"); Value ret = task.get();  CarpetSettings.LOG.error("gotten in future"); return ret;} },
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
        CarpetSettings.LOG.error("in join "+id);
        if (executingThread == null)
        {
            CarpetSettings.LOG.error("not started "+id);
            Value res;
            waitingTreads.put(Thread.currentThread(), 1);
            try
            {
                CarpetSettings.LOG.error(" getting result "+id);
                res = task.get();
                CarpetSettings.LOG.error(" result got "+id);
            }
            finally
            {
                waitingTreads.remove(Thread.currentThread());
            }
            taskFuture.complete(res);
            return res;
        }
        try
        {
            synchronized (waitingTreads)
            {
                if (waitingTreads.containsKey(executingThread))
                    throw new InternalExpressionException("Cannot wait/join on world accessing thread");
                waitingTreads.put(executingThread, 1);
            }
            CarpetSettings.LOG.error("getting future "+id);
            return taskFuture.get();
        }
        catch (InterruptedException | ExecutionException e)
        {
            return Value.NULL;
        }
        finally
        {
            waitingTreads.remove(executingThread);
            CarpetSettings.LOG.error(" future gotten "+id);
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
                executingThread = Thread.currentThread();
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
