package carpet.script.exception;

import carpet.script.value.Value;

import javax.annotation.Nullable;

public class BreakStatement extends ExitStatement
{
    public BreakStatement(@Nullable Value value)
    {
        super(value);
    }
}
