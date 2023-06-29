package carpet.script.exception;

import carpet.script.value.Value;

public class ReturnStatement extends ExitStatement
{
    public ReturnStatement(Value value)
    {
        super(value);
    }
}
