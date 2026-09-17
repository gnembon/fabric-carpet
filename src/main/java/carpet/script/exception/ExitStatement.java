package carpet.script.exception;

import carpet.script.value.Value;

import org.jspecify.annotations.Nullable;

/* Exception thrown to terminate execution mid expression (aka return statement) */
public class ExitStatement extends StacklessRuntimeException
{
    public final @Nullable Value retval;

    public ExitStatement(@Nullable Value value)
    {
        retval = value;
    }
}
