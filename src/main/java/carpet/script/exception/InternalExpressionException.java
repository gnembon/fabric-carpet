package carpet.script.exception;

import carpet.script.value.FunctionValue;

import java.util.ArrayList;
import java.util.List;

/* The internal expression evaluators exception class. */
public class InternalExpressionException extends RuntimeException
{
    public List<FunctionValue> stack = new ArrayList<>();
    public InternalExpressionException(String message)
    {
        super(message);
    }
}
