package carpet.script.exception;

import carpet.script.value.Value;

import org.jspecify.annotations.Nullable;

public class BreakStatement extends ExitStatement
{
    public BreakStatement(@Nullable Value value)
    {
        super(value);
    }
}
