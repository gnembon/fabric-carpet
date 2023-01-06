package carpet.script.utils;

import java.util.function.Supplier;

public class GlocalFlag extends ThreadLocal<Boolean>
{
    private final boolean initial;

    public GlocalFlag(boolean initial)
    {
        this.initial = initial;
    }

    @Override
    public Boolean initialValue()
    {
        return initial;
    }

    /**
     * Allows to thread-safely wrap a call while disabling a global flag and setting it back up right after.
     * @param action - callback to invoke when the wrapping is all setup
     * @param <T> - returned value of that action, whatever that might be
     * @return result of the action
     */
    public <T> T getWhileDisabled(Supplier<T> action)
    {
        return whileValueReturn(!initial, action);
    }

    private <T> T whileValueReturn(boolean what, Supplier<T> action)
    {
        T result;
        boolean previous;
        synchronized (this)
        {
            previous = get();
            set(what);
        }
        try
        {
            result = action.get();
        }
        finally
        {
            set(previous);
        }
        return result;
    }

    public <T> T runIfEnabled(Supplier<T> action)
    {
        synchronized (this)
        {
            if (get() != initial) return null;
            set(!initial);
        }
        T result;
        try
        {
            result = action.get();
        }
        finally
        {
            set(initial);
        }
        return result;
    }
}
