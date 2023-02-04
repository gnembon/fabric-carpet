package carpet.script.exception;

import carpet.script.value.Value;

/* Exception thrown to terminate execution mid expression (aka return statement) */
public class ExitStatement extends StacklessRuntimeException
{
    public final Value retval;

    public ExitStatement(final Value value)
    {
        retval = value;
    }
}
