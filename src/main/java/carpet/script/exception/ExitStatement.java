package carpet.script.exception;

import carpet.script.value.Value;

import javax.annotation.Nullable;

/* Exception thrown to terminate execution mid expression (aka return statement) */
public class ExitStatement extends StacklessRuntimeException
{
    @Nullable
    public final Value retval;

    public ExitStatement(@Nullable Value value)
    {
        retval = value;
    }
}
