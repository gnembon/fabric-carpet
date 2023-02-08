package carpet.script.exception;

import carpet.script.value.Value;

import javax.annotation.Nullable;

public class ContinueStatement extends ExitStatement
{
    public ContinueStatement(@Nullable Value value)
    {
        super(value);
    }
}
